import PhotosUI
import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var weather: WeatherService
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var avatarItem: PhotosPickerItem?
    @State private var isEditingTasks = false
    @State private var themeFeedback = 0
    @State private var cardFeedback = 0
    @State private var avatarFeedback = 0
    @State private var mascotPull: CGFloat = 0

    /// 卡片完全拉出时的高度（含上下留白）。
    private let mascotRevealHeight: CGFloat = 262

    /// 跟随手指、并做软性阻尼的卡片显示高度。
    private var mascotRevealedHeight: CGFloat {
        let damped = mascotPull < mascotRevealHeight
            ? mascotPull
            : mascotRevealHeight + (mascotPull - mascotRevealHeight) * 0.18
        return min(max(damped, 0), mascotRevealHeight + 24)
    }

    var body: some View {
        VStack(spacing: 0) {
            if mascotRevealedHeight > 0.5 {
                MascotDailyStage(
                    recognizedCount: model.todayHistoryCount,
                    wordCount: model.todayWordCount,
                    storyCount: model.todayStoryCount
                )
                .padding(.horizontal, 24)
                .frame(height: mascotRevealedHeight, alignment: .bottom)
                .clipped()
                .opacity(min(Double(mascotRevealedHeight / 80), 1))
                .padding(.bottom, 12)
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    profileRow
                    tasksCard
                    discoveryPager
                }
                .padding(24)
            }
        }
        .overlay(alignment: .topTrailing) {
            MascotPullCord(stretch: mascotRevealedHeight)
                .frame(width: 140, height: 120, alignment: .top)
                .padding(.trailing, 20)
                .opacity(1 - min(Double(mascotRevealedHeight / mascotRevealHeight), 1))
                .contentShape(Rectangle())
                .gesture(mascotPullGesture)
        }
        .background(FeltTheme.cream)
        .foregroundStyle(FeltTheme.ink)
        .navigationBarHidden(true)
        .task { await weather.loadIfNeeded() }
        .onChange(of: avatarItem) { _, item in loadAvatar(item) }
        .onChange(of: model.tasks) { _, _ in model.saveTasks() }
    }

    /// 按住拉绳下拉：实时跟随手指；松手即弹回原状。
    private var mascotPullGesture: some Gesture {
        DragGesture(minimumDistance: 6)
            .onChanged { value in
                mascotPull = max(0, value.translation.height)
            }
            .onEnded { _ in springBackMascot() }
    }

    private func springBackMascot() {
        let animation: Animation = reduceMotion
            ? .easeOut(duration: 0.15)
            : .spring(response: 0.5, dampingFraction: 0.82)
        withAnimation(animation) {
            mascotPull = 0
        }
    }

    // MARK: - 主页面横向快捷入口

    private var discoveryPager: some View {
        let shouldReduceMotion = reduceMotion
        return GeometryReader { geometry in
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 12) {
                    ForEach(HomeDiscoveryPanel.allCases) { panel in
                        Button {
                            cardFeedback += 1
                            model.selectedTab = panel.destination
                        } label: {
                            discoveryCard(panel)
                        }
                        .buttonStyle(FeltPressStyle(pressedScale: 0.975))
                        .frame(width: geometry.size.width - 52)
                        .id(panel)
                        .scrollTransition(.interactive, axis: .horizontal) { content, phase in
                            content
                                .scaleEffect(shouldReduceMotion || phase.isIdentity ? 1 : 0.94)
                                .opacity(shouldReduceMotion || phase.isIdentity ? 1 : 0.82)
                                .offset(y: shouldReduceMotion || phase.isIdentity ? 0 : 8)
                        }
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
        }
        .frame(height: 176)
        .sensoryFeedback(.selection, trigger: cardFeedback)
    }

    private func discoveryCard(_ panel: HomeDiscoveryPanel) -> some View {
        ZStack {
            BundledMascotImage(name: panel.assetName)
                .scaledToFill()
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            LinearGradient(
                colors: [panel.color.opacity(0.98), panel.color.opacity(0.78), panel.color.opacity(0.16)],
                startPoint: .leading,
                endPoint: .trailing
            )

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
                    .opacity(0.78)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .padding(22)
            .padding(.trailing, 70)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .clipShape(RoundedRectangle(cornerRadius: 26))
        .shadow(color: panel.color.opacity(0.22), radius: 14, y: 7)
        .overlay(alignment: .bottomTrailing) {
            Image(systemName: panel.symbol)
                .font(.system(size: 28, weight: .bold))
                .frame(width: 46, height: 46)
                .feltGlassCircle(tint: panel.color.opacity(0.16))
                .padding(8)
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

    /// 点击在浅色/深色间切换；长按可选择跟随时间。
    private var weatherBadge: some View {
        VStack(spacing: 4) {
            Button {
                themeFeedback += 1
                withAnimation(.spring(response: 0.42, dampingFraction: 0.68)) {
                    weather.toggleLightDark()
                }
            } label: {
                Image(systemName: weather.symbol)
                    .font(.system(size: 30))
                    .symbolRenderingMode(.multicolor)
                    .contentTransition(.symbolEffect(.replace))
                    .rotationEffect(.degrees(weather.isDay ? 0 : 12))
                    .frame(width: 64, height: 64)
                    .feltGlassCircle(tint: weather.isDay ? FeltTheme.yellow.opacity(0.16) : FeltTheme.sky.opacity(0.18))
            }
            .buttonStyle(FeltPressStyle(pressedScale: 0.92))
            .contextMenu {
                ForEach(ThemeMode.allCases, id: \.self) { mode in
                    Button {
                        themeFeedback += 1
                        withAnimation(.spring(response: 0.42, dampingFraction: 0.68)) {
                            weather.setThemeMode(mode)
                        }
                    } label: {
                        Label(mode.title, systemImage: weather.themeMode == mode ? "checkmark" : mode.symbol)
                    }
                }
            }
            .accessibilityLabel("主题模式")
            .accessibilityValue(weather.themeMode.title)
            .sensoryFeedback(.selection, trigger: themeFeedback)

            Text(weather.temperature.map { "\($0)°" } ?? "—")
                .font(.system(size: 15, weight: .heavy, design: .rounded))
                .contentTransition(.numericText())
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
        return ZStack(alignment: .bottomTrailing) {
            PhotosPicker(selection: $avatarItem, matching: .images) {
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
            }
            .buttonStyle(.plain)

            if avatarImage == nil {
                avatarBadge(symbol: "plus", color: FeltTheme.orange)
                    .allowsHitTesting(false)
            } else {
                Button {
                    avatarFeedback += 1
                    avatarItem = nil
                    withAnimation(.spring(response: 0.42, dampingFraction: 0.72)) {
                        model.deleteAvatar()
                    }
                } label: {
                    avatarBadge(symbol: "trash.fill", color: .red)
                }
                .buttonStyle(FeltPressStyle(pressedScale: 0.88))
                .accessibilityLabel("删除头像")
            }
        }
        .sensoryFeedback(.success, trigger: avatarFeedback)
    }

    private func avatarBadge(symbol: String, color: Color) -> some View {
        Image(systemName: symbol)
            .font(.system(size: 15, weight: .heavy))
            .foregroundStyle(.white)
            .frame(width: 34, height: 34)
            .background(color, in: Circle())
            .overlay(Circle().stroke(.white, lineWidth: 3))
            .contentTransition(.symbolEffect(.replace))
            .offset(x: 4, y: 4)
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
                withAnimation(.spring(response: 0.42, dampingFraction: 0.72)) {
                    model.setAvatar(image)
                }
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

    var assetName: String {
        switch self {
        case .camera: "card-camera"
        case .stories: "card-stories"
        case .words: "card-words"
        case .history: "card-history"
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
