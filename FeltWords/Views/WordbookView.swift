import SwiftUI

struct WordbookView: View {
    @EnvironmentObject private var model: AppModel

    @State private var isDeleteMode = false
    @State private var speakingWord: String?
    @State private var deletedBatch: [DeletedWord] = []
    @State private var undoTask: Task<Void, Never>?
    @State private var deleteFeedback = 0
    @State private var modeFeedback = 0

    var body: some View {
        List {
            ForEach(model.words) { word in
                wordRow(word)
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .scale(scale: 0.98)),
                        removal: .opacity.combined(with: .scale(scale: 0.94))
                    ))
                    .listRowBackground(FeltTheme.surface)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(FeltTheme.cream)
        .navigationTitle("单词本")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
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
                .accessibilityLabel(isDeleteMode ? "完成删除" : "管理单词")
            }
        }
        .onAppear {
            // 缺图老单词先从同名绘本借现成毛毡图（零 API、即时）。
            withAnimation(.easeInOut(duration: 0.4)) { _ = model.linkStoryImagesToWords() }
        }
        .overlay {
            if model.words.isEmpty {
                MascotEmptyState(title: "还没有单词", description: "识别物品后，把喜欢的英文收藏在这里")
            }
        }
        .safeAreaInset(edge: .bottom) {
            if !deletedBatch.isEmpty {
                undoToast
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

    private func wordRow(_ word: LearnedWord) -> some View {
        HStack(spacing: 16) {
            thumbnail(for: word)
                .frame(width: 72, height: 72)

            VStack(alignment: .leading, spacing: 5) {
                Text(word.word).font(.system(.title2, design: .rounded, weight: .bold))
                Text(word.displayNameZh).foregroundStyle(FeltTheme.secondary)
                Text(word.exampleSentence).font(.caption).foregroundStyle(FeltTheme.secondary)
            }

            Spacer()

            if isDeleteMode {
                Button {
                    deleteFeedback += 1
                    delete(word: word)
                } label: {
                    Image(systemName: "trash.fill")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(width: 38, height: 38)
                        .feltGlassCircle(tint: .red.opacity(0.18))
                }
                .buttonStyle(FeltPressStyle(pressedScale: 0.88))
                .transition(.scale.combined(with: .opacity))
            } else {
                Button {
                    speakingWord = word.word
                    model.speech.speak(word.word)
                } label: {
                    AnimatedSpeakerView(
                        isSpeaking: model.speech.isSpeaking && speakingWord == word.word,
                        tint: FeltTheme.orange,
                        size: 20
                    )
                    .frame(width: 38, height: 38)
                }
                .buttonStyle(FeltPressStyle(pressedScale: 0.9))
                .transition(.scale.combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.32, dampingFraction: 0.76), value: isDeleteMode)
    }

    private var undoToast: some View {
        HStack(spacing: 14) {
            Image(systemName: "trash")
                .foregroundStyle(.red)
            Text("已删除 \(deletedBatch.count) 个单词")
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

    /// 缩略图：优先用单词保存时的毛毡重绘图；没有（旧数据/生成失败）则显示分类毛毡占位，
    /// 点一下即可文生图补一张真毛毡图，生成期间显示加载态。
    @ViewBuilder
    private func thumbnail(for word: LearnedWord) -> some View {
        if let url = word.imageURL {
            StoredImage(url: url)
                .scaledToFill()
                .frame(width: 72, height: 72)
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(FeltTheme.ink.opacity(0.08), lineWidth: 1))
        } else if model.backfillingWordIDs.contains(word.id) {
            let style = Self.placeholderStyle(for: word.category)
            ZStack {
                FeltObject(symbol: style.symbol, color: style.color)
                RoundedRectangle(cornerRadius: 18).fill(.ultraThinMaterial)
                ProgressView()
            }
        } else {
            let style = Self.placeholderStyle(for: word.category)
            Button { model.backfillImage(for: word.id) } label: {
                ZStack(alignment: .bottom) {
                    FeltObject(symbol: style.symbol, color: style.color)
                    Text("点我补图")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6).padding(.vertical, 3)
                        .background(FeltTheme.ink.opacity(0.55), in: Capsule())
                        .padding(.bottom, 5)
                }
            }
            .buttonStyle(.plain)
        }
    }

    /// 按物品分类给毛毡占位选符号与底色，让没有真图的单词也各有性格。
    private static func placeholderStyle(for category: String) -> (symbol: String, color: Color) {
        switch category.lowercased() {
        case let c where c.contains("food") || c.contains("fruit") || c.contains("食") || c.contains("水果"):
            return ("fork.knife", FeltTheme.orange)
        case let c where c.contains("animal") || c.contains("动物"):
            return ("pawprint.fill", FeltTheme.mint)
        case let c where c.contains("plant") || c.contains("nature") || c.contains("植") || c.contains("自然"):
            return ("leaf.fill", FeltTheme.mint)
        case let c where c.contains("toy") || c.contains("玩"):
            return ("teddybear.fill", FeltTheme.pink)
        case let c where c.contains("water") || c.contains("sky") || c.contains("水") || c.contains("天"):
            return ("drop.fill", FeltTheme.sky)
        default:
            return ("sparkles", FeltTheme.yellow)
        }
    }

    private func delete(word: LearnedWord) {
        guard let index = model.words.firstIndex(where: { $0.id == word.id }) else { return }
        let originalIndex = index + deletedBatch.filter { $0.index <= index }.count
        deletedBatch.append(DeletedWord(index: originalIndex, word: word))
        withAnimation(.spring(response: 0.25, dampingFraction: 0.8)) {
            model.deleteWord(id: word.id)
        }
        scheduleUndoExpiry()
    }

    private func undoDelete() {
        undoTask?.cancel()
        let restored = deletedBatch.map { (index: $0.index, word: $0.word) }
        withAnimation(.spring(response: 0.38, dampingFraction: 0.76)) {
            model.restoreWords(restored)
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

private struct DeletedWord {
    let index: Int
    let word: LearnedWord
}
