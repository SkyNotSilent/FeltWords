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

@MainActor
final class WeatherService: ObservableObject {
    private static let themeModeKey = "feltwords.themeMode"
    private static let cacheTemperature = "feltwords.weather.temperature"
    private static let cacheCity = "feltwords.weather.city"
    private static let cacheWeatherCode = "feltwords.weather.weatherCode"
    private static let cacheIsDay = "feltwords.weather.isDay"

    @Published private(set) var temperature: Int?
    @Published private(set) var city: String?
    @Published private(set) var didLoad = false
    @Published private(set) var currentWeatherCode: Int = 0
    @Published private(set) var themeMode: ThemeMode {
        didSet { UserDefaults.standard.set(themeMode.rawValue, forKey: Self.themeModeKey) }
    }

    var isDay: Bool {
        switch themeMode {
        case .automatic: _isDay
        case .light: true
        case .dark: false
        }
    }

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
        themeMode = ThemeMode(rawValue: stored ?? "") ?? .light

        let defaults = UserDefaults.standard
        if let cachedCity = defaults.string(forKey: Self.cacheCity) {
            city = cachedCity
            temperature = defaults.object(forKey: Self.cacheTemperature) as? Int
            currentWeatherCode = defaults.integer(forKey: Self.cacheWeatherCode)
            _isDay = defaults.bool(forKey: Self.cacheIsDay)
            didLoad = true
        }
    }

    func toggleLightDark() {
        setThemeMode(isDay ? .dark : .light)
    }

    func setThemeMode(_ mode: ThemeMode) {
        themeMode = mode
    }

    func loadIfNeeded() async {
        if didLoad {
            await refreshInBackground()
            return
        }
        await load()
    }

    func load() async {
        do {
            let location = try await fetchLocation()
            let weather = try await fetchWeather(latitude: location.latitude, longitude: location.longitude)
            applyWeather(weather, city: location.city)
        } catch {
            // 失败时保持缓存值
        }
    }

    private func refreshInBackground() async {
        do {
            let location = try await fetchLocation()
            let weather = try await fetchWeather(latitude: location.latitude, longitude: location.longitude)
            applyWeather(weather, city: location.city)
        } catch {
            // 静默刷新失败，保持缓存
        }
    }

    private func applyWeather(_ weather: MeteoResponse, city: String?) {
        let rawDay = weather.current.is_day != 0
        temperature = Int(weather.current.temperature_2m.rounded())
        _isDay = rawDay
        currentWeatherCode = weather.current.weather_code
        self.city = city
        didLoad = true

        let defaults = UserDefaults.standard
        defaults.set(temperature, forKey: Self.cacheTemperature)
        defaults.set(city, forKey: Self.cacheCity)
        defaults.set(currentWeatherCode, forKey: Self.cacheWeatherCode)
        defaults.set(rawDay, forKey: Self.cacheIsDay)
    }

    private func fetchLocation() async throws -> IPLocation {
        do {
            let url = URL(string: "https://ipapi.co/json/")!
            let (data, response) = try await session.data(from: url)
            try Self.validate(response)
            return try JSONDecoder().decode(IPLocation.self, from: data)
        } catch {
            return try await fetchLocationFallback()
        }
    }

    private func fetchLocationFallback() async throws -> IPLocation {
        let url = URL(string: "https://ipwho.is/")!
        let (data, response) = try await session.data(from: url)
        try Self.validate(response)
        let result = try JSONDecoder().decode(IPWhoIsLocation.self, from: data)
        return IPLocation(city: result.city, latitude: result.latitude, longitude: result.longitude)
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

private struct IPWhoIsLocation: Decodable {
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
