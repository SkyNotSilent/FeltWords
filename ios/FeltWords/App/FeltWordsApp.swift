import SwiftUI

@main
struct FeltWordsApp: App {
    @StateObject private var model = AppModel()
    @StateObject private var weather = WeatherService()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(model)
                .environmentObject(weather)
                // 昼夜驱动全局浅色/深色模式：白天浅色、夜晚深色。
                .preferredColorScheme(weather.isDay ? .light : .dark)
                .task { await weather.loadIfNeeded() }
        }
    }
}

