import Foundation

enum ThemeMode: String, CaseIterable {
    case automatic
    case light
    case dark

    var title: String {
        switch self {
        case .automatic: "跟随时间"
        case .light: "浅色"
        case .dark: "深色"
        }
    }

    var symbol: String {
        switch self {
        case .automatic: "clock.arrow.circlepath"
        case .light: "sun.max.fill"
        case .dark: "moon.stars.fill"
        }
    }
}

/// 通过手机出口 IP 定位城市，再取当前气温。两个接口都是免费 HTTPS、无需密钥：
/// - ipapi.co：IP → 城市 + 经纬度
/// - open-meteo：经纬度 → 当前气温 + 天气代码
@MainActor
final class WeatherService: ObservableObject {
    private static let themeModeKey = "feltwords.themeMode"

    @Published private(set) var temperature: Int?
    @Published private(set) var city: String?
    @Published private(set) var didLoad = false
    /// 原始天气代码，用于计算 symbol。
    @Published private(set) var currentWeatherCode: Int = 0
    @Published private(set) var themeMode: ThemeMode {
        didSet { UserDefaults.standard.set(themeMode.rawValue, forKey: Self.themeModeKey) }
    }

    /// 当前应显示的昼夜模式（受手动强制或自动昼夜影响）。
    var isDay: Bool {
        switch themeMode {
        case .automatic: _isDay
        case .light: true
        case .dark: false
        }
    }

    /// 当前应显示的 symbol（随昼夜和天气代码变化）。
    var symbol: String {
        Self.symbol(forWMOCode: currentWeatherCode, isDay: isDay)
    }

    private var _isDay = true

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 12
        return URLSession(configuration: config)
    }()

    init() {
        let stored = UserDefaults.standard.string(forKey: Self.themeModeKey)
        themeMode = ThemeMode(rawValue: stored ?? "") ?? .automatic
    }

    /// 普通点击始终在当前有效浅色/深色之间切换，确保每次点击都有可见变化。
    func toggleLightDark() {
        setThemeMode(isDay ? .dark : .light)
    }

    func setThemeMode(_ mode: ThemeMode) {
        themeMode = mode
    }

    func loadIfNeeded() async {
        guard !didLoad else { return }
        await load()
    }

    func load() async {
        do {
            let location = try await fetchLocation()
            let weather = try await fetchWeather(latitude: location.latitude, longitude: location.longitude)
            let rawDay = weather.current.is_day != 0
            temperature = Int(weather.current.temperature_2m.rounded())
            _isDay = rawDay
            currentWeatherCode = weather.current.weather_code
            city = location.city
            didLoad = true
        } catch {
            // 失败时保持空值，UI 显示占位；didLoad 仍为 false 以便下次重试。
        }
    }

    private func fetchLocation() async throws -> IPLocation {
        let url = URL(string: "https://ipapi.co/json/")!
        let (data, response) = try await session.data(from: url)
        try Self.validate(response)
        return try JSONDecoder().decode(IPLocation.self, from: data)
    }

    private func fetchWeather(latitude: Double, longitude: Double) async throws -> MeteoResponse {
        var components = URLComponents(string: "https://api.open-meteo.com/v1/forecast")!
        components.queryItems = [
            .init(name: "latitude", value: String(latitude)),
            .init(name: "longitude", value: String(longitude)),
            .init(name: "current", value: "temperature_2m,weather_code,is_day")
        ]
        let (data, response) = try await session.data(from: components.url!)
        try Self.validate(response)
        return try JSONDecoder().decode(MeteoResponse.self, from: data)
    }

    private static func validate(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
            throw URLError(.badServerResponse)
        }
    }

    /// WMO 天气代码 → SF Symbol，区分昼夜（晴天白天太阳、夜晚月亮）。
    private static func symbol(forWMOCode code: Int, isDay: Bool) -> String {
        switch code {
        case 0: return isDay ? "sun.max.fill" : "moon.stars.fill"
        case 1, 2, 3: return isDay ? "cloud.sun.fill" : "cloud.moon.fill"
        case 45, 48: return "cloud.fog.fill"
        case 51...67: return isDay ? "cloud.rain.fill" : "cloud.moon.rain.fill"
        case 71...77: return "snowflake"
        case 80...82: return "cloud.heavyrain.fill"
        case 95...99: return "cloud.bolt.rain.fill"
        default: return "cloud.fill"
        }
    }
}

private struct IPLocation: Decodable {
    let city: String?
    let latitude: Double
    let longitude: Double
}

private struct MeteoResponse: Decodable {
    let current: Current
    struct Current: Decodable {
        let temperature_2m: Double
        let weather_code: Int
        let is_day: Int
    }
}
