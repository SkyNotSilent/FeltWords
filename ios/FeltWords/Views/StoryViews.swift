import SwiftUI

struct StoryLibraryView: View {
    @EnvironmentObject private var model: AppModel
    @State private var showArchive = false
    @State private var isDeleteMode = false
    @State private var deletedBatch: [DeletedStory] = []
    @State private var undoTask: Task<Void, Never>?
    @State private var deleteFeedback = 0
    @State private var modeFeedback = 0

    private let columns = [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)]

    /// 顶部只展示最近 4 本；更早的收进可展开的"故事库"。
    private var recentStories: [Storybook] { Array(model.stories.prefix(4)) }
    private var archivedStories: [Storybook] { Array(model.stories.dropFirst(4)) }

    var body: some View {
        ScrollView {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("我的绘本")
                        .font(.system(size: 34, weight: .heavy, design: .rounded))
                    Text("共 \(model.stories.count) 本小故事")
                        .font(.subheadline)
                        .foregroundStyle(FeltTheme.secondary)
                }
                Spacer()
                deleteModeButton
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, 8)

            LazyVGrid(columns: columns, spacing: 14) {
                ForEach(model.generatingStories) { job in
                    GeneratingCard(job: job)
                        .transition(.scale(scale: 0.6).combined(with: .opacity))
                }
                ForEach(recentStories) { story in
                    storyTile(story)
                        .transition(.scale(scale: 0.6).combined(with: .opacity))
                }
            }
            .padding(20)
            .animation(.spring(response: 0.45, dampingFraction: 0.7), value: model.stories.count)
            .animation(.spring(response: 0.45, dampingFraction: 0.7), value: model.generatingStories.count)

            if !archivedStories.isEmpty {
                archiveSection
            }
        }
        .background(FeltTheme.cream)
        .navigationTitle("我的绘本")
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(for: Storybook.self) { StoryReaderView(story: $0) }
        .overlay {
            if model.stories.isEmpty && model.generatingStories.isEmpty {
                MascotEmptyState(title: "还没有绘本", description: "拍一个物品，和毛毛一起生成第一本小故事")
            }
        }
        .overlay(alignment: .bottom) {
            if !deletedBatch.isEmpty {
                undoToast
                    .padding(.bottom, 78)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.38, dampingFraction: 0.76), value: deletedBatch.isEmpty)
        .sensoryFeedback(.warning, trigger: deleteFeedback)
        .sensoryFeedback(.selection, trigger: modeFeedback)
        .onDisappear {
            undoTask?.cancel()
            deletedBatch.removeAll()
        }
    }

    private var deleteModeButton: some View {
        Button {
            modeFeedback += 1
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                isDeleteMode.toggle()
            }
        } label: {
            Image(systemName: isDeleteMode ? "checkmark" : "trash")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(isDeleteMode ? .red : FeltTheme.orange)
                .contentTransition(.symbolEffect(.replace))
                .frame(width: 38, height: 38)
                .feltGlassCircle(tint: isDeleteMode ? .red.opacity(0.12) : FeltTheme.orange.opacity(0.12))
        }
        .buttonStyle(FeltPressStyle(pressedScale: 0.9))
        .accessibilityLabel(isDeleteMode ? "完成删除" : "管理绘本")
    }

    /// 单张绘本卡片：普通状态点进阅读，删除状态持续轻微抖动。
    private func storyTile(_ story: Storybook) -> some View {
        ZStack(alignment: .topTrailing) {
            NavigationLink(value: story) { StoryCard(story: story) }
                .buttonStyle(CardPressStyle())
                .disabled(isDeleteMode)
                .rotationEffect(.degrees(isDeleteMode ? 1.2 : 0))
                .animation(
                    isDeleteMode
                        ? .easeInOut(duration: 0.12).repeatForever(autoreverses: true)
                        : .spring(response: 0.28, dampingFraction: 0.7),
                    value: isDeleteMode
                )

            if isDeleteMode {
                Button {
                    deleteFeedback += 1
                    delete(story: story)
                } label: {
                    Image(systemName: "trash.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(width: 34, height: 34)
                        .feltGlassCircle(tint: .red.opacity(0.2))
                }
                .buttonStyle(FeltPressStyle(pressedScale: 0.86))
                .padding(7)
                .transition(.scale.combined(with: .opacity))
            }
        }
    }

    private var archiveSection: some View {
        VStack(spacing: 14) {
            Button {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.75)) { showArchive.toggle() }
            } label: {
                HStack {
                    Image(systemName: "books.vertical.fill")
                    Text("故事库")
                    Text("\(archivedStories.count)")
                        .font(.caption.bold())
                        .padding(.horizontal, 8).padding(.vertical, 2)
                        .background(FeltTheme.mint, in: Capsule())
                    Spacer()
                    Image(systemName: "chevron.down")
                        .rotationEffect(.degrees(showArchive ? 180 : 0))
                }
                .font(.system(.headline, design: .rounded, weight: .bold))
                .foregroundStyle(FeltTheme.ink)
                .padding(16)
                .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 18))
            }
            .buttonStyle(.plain)

            if showArchive {
                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(archivedStories) { story in
                        storyTile(story)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
    }

    private var undoToast: some View {
        HStack(spacing: 14) {
            Image(systemName: "trash")
                .foregroundStyle(.red)
            Text("已删除 \(deletedBatch.count) 本绘本")
                .font(.subheadline.bold())
            Spacer()
            Button("撤销") { undoDelete() }
                .font(.subheadline.bold())
                .foregroundStyle(FeltTheme.orange)
        }
        .padding(.horizontal, 18)
        .frame(height: 54)
        .feltGlassCapsule(tint: FeltTheme.surface.opacity(0.18))
        .padding(.horizontal, 18)
        .padding(.bottom, 6)
    }

    private func delete(story: Storybook) {
        guard let index = model.stories.firstIndex(where: { $0.id == story.id }) else { return }
        let originalIndex = index + deletedBatch.filter { $0.index <= index }.count
        deletedBatch.append(DeletedStory(index: originalIndex, story: story))
        model.deleteStory(id: story.id)
        scheduleUndoExpiry()
    }

    private func undoDelete() {
        undoTask?.cancel()
        let restored = deletedBatch.map { (index: $0.index, story: $0.story) }
        withAnimation(.spring(response: 0.38, dampingFraction: 0.76)) {
            model.restoreStories(restored)
            deletedBatch.removeAll()
        }
        modeFeedback += 1
    }

    private func scheduleUndoExpiry() {
        undoTask?.cancel()
        undoTask = Task {
            try? await Task.sleep(for: .seconds(5))
            guard !Task.isCancelled else { return }
            withAnimation(.easeOut(duration: 0.24)) {
                deletedBatch.removeAll()
            }
        }
    }
}

private struct DeletedStory {
    let index: Int
    let story: Storybook
}

private struct StoryCard: View {
    let story: Storybook

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            StoryCover(url: story.pages.first?.imageURL)
                .aspectRatio(4.0 / 3.0, contentMode: .fill)
                .frame(maxWidth: .infinity)
                .frame(height: 112)
                .clipShape(RoundedRectangle(cornerRadius: 16))

            VStack(alignment: .leading, spacing: 8) {
                Text(story.title)
                    .font(.system(.subheadline, design: .rounded, weight: .heavy))
                    .foregroundStyle(FeltTheme.ink)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 6) {
                    Text(story.focusWord)
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .foregroundStyle(FeltTheme.ink)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(FeltTheme.yellow, in: Capsule())
                    Spacer()
                    HStack(spacing: 3) {
                        Image(systemName: "book.pages.fill").font(.system(size: 9))
                        Text("\(story.pages.count)")
                    }
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(FeltTheme.secondary)
                }
            }
            .padding(.horizontal, 4)
        }
        .padding(10)
        .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 22))
        .shadow(color: FeltTheme.ink.opacity(0.1), radius: 6, y: 3)
    }
}

/// "生成中"占位卡片：显示参考图 + 进度，不可点进；失败时轻点重试、长按移除。
private struct GeneratingCard: View {
    @EnvironmentObject private var model: AppModel
    let job: GeneratingStory

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack {
                StoryCover(url: job.coverURL)
                    .aspectRatio(4.0 / 3.0, contentMode: .fill)
                    .frame(maxWidth: .infinity)
                    .frame(height: 112)
                Rectangle().fill(.black.opacity(0.4))
                if job.failed {
                    Image(systemName: "arrow.clockwise.circle.fill")
                        .font(.largeTitle).foregroundStyle(.white)
                } else {
                    ProgressView().tint(.white)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 16))

            VStack(alignment: .leading, spacing: 8) {
                Text(job.failed ? "生成失败" : "正在画绘本…")
                    .font(.system(.subheadline, design: .rounded, weight: .heavy))
                    .foregroundStyle(FeltTheme.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 6) {
                    Text(job.focusWord)
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .foregroundStyle(FeltTheme.ink)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(FeltTheme.yellow, in: Capsule())
                    Spacer()
                    Text(job.failed ? "轻点重试" : "第 \(min(job.progressDone + 1, job.progressTotal))/\(job.progressTotal) 页")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(FeltTheme.secondary)
                }
            }
            .padding(.horizontal, 4)
        }
        .padding(10)
        .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 22))
        .shadow(color: FeltTheme.ink.opacity(0.1), radius: 6, y: 3)
        .onTapGesture { if job.failed { model.retryStoryGeneration(id: job.id) } }
        .onLongPressGesture { if job.failed { model.dismissStoryJob(id: job.id) } }
    }
}

/// 卡片按压缩放反馈。
private struct CardPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

struct StoryReaderView: View {
    @EnvironmentObject private var model: AppModel
    let story: Storybook
    @State private var page = 0
    @State private var isAutoPlaying = false
    @State private var showZh = false

    var body: some View {
        VStack(spacing: 14) {
            TabView(selection: $page) {
                ForEach(Array(story.pages.enumerated()), id: \.element.id) { index, item in
                    pageView(item).tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            pageDots
            controls
        }
        .padding(.bottom, 18)
        .background(
            LinearGradient(colors: [FeltTheme.sky, FeltTheme.cream],
                           startPoint: .top, endPoint: .bottom).ignoresSafeArea()
        )
        .foregroundStyle(FeltTheme.ink)
        .navigationTitle(story.title)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: page) { _, _ in
            showZh = false
            if !isAutoPlaying { model.speech.speak(story.pages[page].sentence) }
        }
        .onAppear { model.speech.speak(story.pages[page].sentence) }
        .onDisappear { model.speech.stop() }
    }

    private func pageView(_ item: StoryPage) -> some View {
        VStack(spacing: 20) {
            StoryCover(url: item.imageURL)
                .frame(maxWidth: .infinity)
                .frame(height: 380)
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .overlay(RoundedRectangle(cornerRadius: 28).stroke(.white, lineWidth: 4))
                .shadow(color: FeltTheme.ink.opacity(0.16), radius: 12, y: 6)

            VStack(spacing: 0) {
                Text(item.sentence)
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
                    .padding(.horizontal, 16)

                if showZh && !item.sentenceZh.isEmpty {
                    Text(item.sentenceZh)
                        .font(.system(size: 18, weight: .medium, design: .rounded))
                        .foregroundStyle(FeltTheme.secondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 14)
                        .padding(.horizontal, 16)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                }

                if !showZh && !item.sentenceZh.isEmpty {
                    Text("点击让毛毛为你翻译 ✨")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(FeltTheme.ink.opacity(0.32))
                        .padding(.bottom, 10)
                }
            }
            .background(FeltTheme.surface.opacity(0.85), in: RoundedRectangle(cornerRadius: 22))
            .onTapGesture {
                if !item.sentenceZh.isEmpty {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                        showZh.toggle()
                    }
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
    }

    private var pageDots: some View {
        HStack(spacing: 8) {
            ForEach(story.pages.indices, id: \.self) { index in
                Capsule()
                    .fill(index == page ? FeltTheme.orange : FeltTheme.ink.opacity(0.18))
                    .frame(width: index == page ? 22 : 8, height: 8)
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: page)
            }
        }
    }

    private var controls: some View {
        HStack {
            Button { goPrevious() } label: {
                Image(systemName: "chevron.left").secondaryControl()
            }
            .disabled(page == 0)

            Spacer()

            Button { togglePlay() } label: {
                Image(systemName: isAutoPlaying ? "pause.fill" : "play.fill")
                    .font(.title.bold())
                    .foregroundStyle(FeltTheme.ink)
                    .frame(width: 72, height: 72)
                    .background(FeltTheme.yellow, in: Circle())
                    .shadow(color: FeltTheme.ink.opacity(0.18), radius: 8, y: 4)
            }

            Spacer()

            Button { goNext() } label: {
                Image(systemName: "chevron.right").secondaryControl()
            }
            .disabled(page == story.pages.count - 1)
        }
        .padding(.horizontal, 44)
    }

    // MARK: - 自动播放

    private func togglePlay() {
        if isAutoPlaying {
            isAutoPlaying = false
            model.speech.stop()
        } else {
            isAutoPlaying = true
            if page == story.pages.count - 1 {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.78)) {
                    page = 0
                }
            }
            playCurrentThenAdvance()
        }
    }

    private func playCurrentThenAdvance() {
        model.speech.speak(story.pages[page].sentence) {
            guard isAutoPlaying else { return }
            if page < story.pages.count - 1 {
                withAnimation { page += 1 }
                playCurrentThenAdvance()
            } else {
                isAutoPlaying = false
            }
        }
    }

    private func goPrevious() {
        isAutoPlaying = false
        withAnimation { page = max(0, page - 1) }
    }

    private func goNext() {
        isAutoPlaying = false
        withAnimation { page = min(story.pages.count - 1, page + 1) }
    }
}

/// 绘本封面/插画图，统一占位与填充。
private struct StoryCover: View {
    let url: URL?

    var body: some View {
        if let url {
            StoredImage(url: url).scaledToFill()
        } else {
            FeltObject(symbol: "book.pages.fill", color: FeltTheme.mint)
        }
    }
}

private extension Image {
    func secondaryControl() -> some View {
        self.font(.title2.bold())
            .foregroundStyle(FeltTheme.ink.opacity(0.7))
            .frame(width: 52, height: 52)
            .background(FeltTheme.surface.opacity(0.7), in: Circle())
    }
}
