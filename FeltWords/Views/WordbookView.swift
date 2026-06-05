import SwiftUI

struct WordbookView: View {
    @EnvironmentObject private var model: AppModel

    @State private var isDeleteMode = false

    var body: some View {
        List {
            if isDeleteMode {
                // 删除模式：用 section 区分，每个卡片有删除按钮
                ForEach(model.words) { word in
                    wordDeleteRow(word)
                }
            } else {
                ForEach(model.words) { word in
                    Button {
                        model.speech.speak(word.word)
                    } label: {
                        HStack(spacing: 16) {
                            thumbnail(for: word)
                                .frame(width: 72, height: 72)
                            VStack(alignment: .leading, spacing: 5) {
                                Text(word.word).font(.system(.title2, design: .rounded, weight: .bold))
                                Text(word.displayNameZh).foregroundStyle(FeltTheme.secondary)
                                Text(word.exampleSentence).font(.caption).foregroundStyle(FeltTheme.secondary)
                            }
                            Spacer()
                            Image(systemName: "speaker.wave.2.fill").foregroundStyle(FeltTheme.orange)
                        }
                    }
                    .buttonStyle(.plain)
                    .listRowBackground(FeltTheme.surface)
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(FeltTheme.cream)
        .navigationTitle("单词本")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                        isDeleteMode.toggle()
                    }
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(isDeleteMode ? .red : FeltTheme.orange)
                }
                .buttonStyle(.plain)
            }
        }
        .onAppear {
            // 缺图老单词先从同名绘本借现成毛毡图（零 API、即时）。
            withAnimation(.easeInOut(duration: 0.4)) { _ = model.linkStoryImagesToWords() }
        }
        .overlay {
            if model.words.isEmpty {
                ContentUnavailableView("还没有单词", systemImage: "textformat.abc", description: Text("识别物品后，把单词收藏在这里"))
            }
        }
    }

    /// 删除模式下的单词行：左侧内容 + 右侧红色删除按钮
    private func wordDeleteRow(_ word: LearnedWord) -> some View {
        HStack(spacing: 12) {
            thumbnail(for: word)
                .frame(width: 56, height: 56)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(FeltTheme.ink.opacity(0.08), lineWidth: 1))

            VStack(alignment: .leading, spacing: 3) {
                Text(word.word).font(.system(.body, design: .rounded, weight: .bold))
                Text(word.displayNameZh).font(.caption).foregroundStyle(FeltTheme.secondary)
            }

            Spacer()

            Button {
                withAnimation(.spring(response: 0.25, dampingFraction: 0.75)) {
                    delete(word: word)
                }
            } label: {
                Image(systemName: "trash")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.red, in: Circle())
            }
        }
        .padding(.vertical, 4)
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
        withAnimation(.spring(response: 0.25, dampingFraction: 0.8)) {
            model.words.removeAll { $0.id == word.id }
        }
        LocalStore.save(model.words)
    }
}
