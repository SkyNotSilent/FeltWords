import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        TabView(selection: $model.selectedTab) {
            NavigationStack { HomeView() }
                .tabItem { Label("首页", systemImage: "house.fill") }
                .tag(AppTab.home)
            NavigationStack { CameraScreen() }
                .tabItem { Label("拍一拍", systemImage: "camera.fill") }
                .tag(AppTab.camera)
            NavigationStack { StoryLibraryView() }
                .tabItem { Label("绘本", systemImage: "book.fill") }
                .tag(AppTab.stories)
            NavigationStack { WordbookView() }
                .tabItem { Label("单词", systemImage: "textformat.abc") }
                .tag(AppTab.words)
            NavigationStack { HistoryView() }
                .tabItem { Label("历史", systemImage: "clock.arrow.circlepath") }
                .tag(AppTab.history)
        }
        .tint(FeltTheme.orange)
    }
}
