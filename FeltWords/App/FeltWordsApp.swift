import SwiftUI

@main
struct FeltWordsApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environmentObject(model)
                .preferredColorScheme(.light)
        }
    }
}

