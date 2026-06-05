import PhotosUI
import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var weather: WeatherService

    @State private var avatarItem: PhotosPickerItem?
    @State private var isEditingTasks = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                header
                profileRow
                tasksCard
                discoveryPager
            }
            .padding(24)
        }
        .background(FeltTheme.cream)
        .foregroundStyle(FeltTheme.ink)
        .navigationBarHidden(true)
        .task { await weather.loadIfNeeded() }
        .onChange(of: avatarItem) { _, item in loadAvatar(item) }
        .onChange(of: model.tasks) { _, _ in model.saveTasks() }
    }

    // MARK: - 主页面横向快捷入口

    private var discoveryPager: some View {
        GeometryReader { geometry in
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 12) {
                    ForEach(HomeDiscoveryPanel.allCases) { panel in
                        Button { model.selectedTab = panel.destination } label: {
                            discoveryCard(panel)
                        }
                        .buttonStyle(.plain)
                        .frame(width: geometry.size.width - 52)
                        .id(panel)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
        }
        .frame(height: 176)
    }

    private func discoveryCard(_ panel: HomeDiscoveryPanel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: panel.swipeSymbol)
                Text(panel.swipeHint)
                Spacer()
            }
            .font(.caption.bold())

            Text(panel.title)
                .font(.system(size: 28, weight: .heavy, design: .rounded))

            Text(summary(for: panel))
                .font(.subheadline)
                .lineLimit(2)
                .opacity(0.72)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(22)
        .padding(.trailing, 54)
        .background(panel.color, in: RoundedRectangle(cornerRadius: 26))
        .overlay(alignment: .trailing) {
            Image(systemName: panel.symbol)
                .font(.system(size: 28, weight: .bold))
                .frame(width: 46, height: 46)
                .background(FeltTheme.surface.opacity(0.78), in: Circle())
                .padding(.trailing, 7)
        }
    }

    private func summary(for panel: HomeDiscoveryPanel) -> String {
        switch panel {
        case .camera:
            return "拍下身边的东西，认识一个新英文"
        case .stories:
            return model.stories.isEmpty ? "还没有绘本，去生成第一本小故事" : "已经收藏 \(model.stories.count) 本小故事"
        case .words:
            return model.words.isEmpty ? "把喜欢的英文收藏到这里" : "已经认识 \(model.words.count) 个英文"
        case .history:
            guard let latest = model.history.first else {
                return "识别完成后会自动保存，按时间排好"
            }
            return "最近识别：\(latest.result.word) · 共 \(model.history.count) 条"
        }
    }

    // MARK: - 问候 + 右上角天气

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Hi, 小悠好～")
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                Text("今天也去发现一个新单词")
                    .foregroundStyle(FeltTheme.secondary)
            }
            Spacer()
            weatherBadge
        }
    }

    /// 右上角天气：白天太阳、夜晚月亮，下面是气温。
    private var weatherBadge: some View {
        VStack(spacing: 4) {
            Image(systemName: weather.symbol)
                .font(.system(size: 30))
                .symbolRenderingMode(.multicolor)
                .frame(width: 64, height: 64)
                .background(weather.isDay ? FeltTheme.surface : FeltTheme.sky,
                            in: Circle())
                .overlay(Circle().stroke(FeltTheme.surface, lineWidth: 3))
            Text(weather.temperature.map { "\($0)°" } ?? "—")
                .font(.system(size: 15, weight: .heavy, design: .rounded))
        }
    }

    // MARK: - 大头像 + 并列日期

    private var profileRow: some View {
        HStack(alignment: .center, spacing: 18) {
            avatarPicker
            VStack(alignment: .leading, spacing: 6) {
                Text(Self.weekdayText)
                    .font(.headline)
                    .foregroundStyle(FeltTheme.secondary)
                Text(Self.dateText)
                    .font(.system(size: 34, weight: .heavy, design: .rounded))
                Label(weather.city ?? "定位中…", systemImage: "location.fill")
                    .font(.subheadline)
                    .foregroundStyle(FeltTheme.secondary)
                    .lineLimit(1)
            }
            Spacer()
        }
    }

    private var avatarPicker: some View {
        let avatarImage = model.avatarImage
        return PhotosPicker(selection: $avatarItem, matching: .images) {
            ZStack(alignment: .bottomTrailing) {
                Group {
                    if let avatar = avatarImage {
                        Image(uiImage: avatar).resizable().scaledToFill()
                            .frame(width: 190, height: 190)
                            .clipShape(RoundedRectangle(cornerRadius: 30))
                            .overlay(RoundedRectangle(cornerRadius: 30).stroke(.white, lineWidth: 4))
                    } else {
                        // 默认占位沿用毛毡小熊。
                        FeltObject(symbol: "teddybear.fill", color: FeltTheme.orange)
                            .frame(width: 190, height: 190)
                    }
                }
                .shadow(color: FeltTheme.ink.opacity(0.14), radius: 8, y: 4)

                // 右下角上传加号。
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(width: 34, height: 34)
                    .background(FeltTheme.orange, in: Circle())
                    .overlay(Circle().stroke(.white, lineWidth: 3))
                    .offset(x: 4, y: 4)
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: - 今日任务（可编辑）

    private var tasksCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("今日任务").font(.title3.bold())
                Spacer()
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { isEditingTasks.toggle() }
                } label: {
                    Text(isEditingTasks ? "完成" : "编辑")
                        .font(.subheadline.bold())
                        .foregroundStyle(FeltTheme.orange)
                }
            }

            ForEach($model.tasks) { $task in
                HStack(spacing: 12) {
                    Image(systemName: task.icon)
                        .font(.title3)
                        .foregroundStyle(FeltTheme.ink)
                        .frame(width: 28)

                    (Text(task.prefix)
                        + Text("\(task.count)").fontWeight(.heavy).foregroundColor(FeltTheme.orange)
                        + Text(task.suffix))
                        .font(.system(.body, design: .rounded))

                    Spacer()

                    if isEditingTasks {
                        Stepper("", value: $task.count, in: 1...99)
                            .labelsHidden()
                    }
                }
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 24))
    }

    // MARK: - Helpers

    private func loadAvatar(_ item: PhotosPickerItem?) {
        guard let item else { return }
        Task {
            if let data = try? await item.loadTransferable(type: Data.self),
               let image = UIImage(data: data) {
                model.setAvatar(image)
            }
        }
    }

    private static var dateText: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "M月d日"
        return formatter.string(from: Date())
    }

    private static var weekdayText: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "EEEE"
        return formatter.string(from: Date())
    }
}

private enum HomeDiscoveryPanel: Int, CaseIterable, Identifiable {
    case camera
    case stories
    case words
    case history

    var id: Self { self }

    var destination: AppTab {
        switch self {
        case .camera: .camera
        case .stories: .stories
        case .words: .words
        case .history: .history
        }
    }

    var title: String {
        switch self {
        case .camera: "开始拍照"
        case .stories: "我的绘本"
        case .words: "单词本"
        case .history: "历史记录"
        }
    }

    var symbol: String {
        switch self {
        case .camera: "camera.fill"
        case .stories: "book.fill"
        case .words: "textformat.abc"
        case .history: "clock.arrow.circlepath"
        }
    }

    var color: Color {
        switch self {
        case .camera: FeltTheme.orange
        case .stories: FeltTheme.mint
        case .words: FeltTheme.pink
        case .history: FeltTheme.sky
        }
    }

    var swipeSymbol: String {
        switch self {
        case .camera: "arrow.left"
        case .history: "arrow.right"
        case .stories, .words: "arrow.left.and.right"
        }
    }

    var swipeHint: String {
        switch self {
        case .camera: "往左滑看我的绘本"
        case .stories, .words: "左右滑查看更多"
        case .history: "往右滑看单词本"
        }
    }
}
