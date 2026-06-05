import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                ForEach(model.history) { item in
                    historyCard(item)
                }
            }
            .padding(20)
        }
        .background(FeltTheme.cream)
        .navigationTitle("历史记录")
        .overlay {
            if model.history.isEmpty {
                ContentUnavailableView(
                    "还没有识别记录",
                    systemImage: "clock.arrow.circlepath",
                    description: Text("拍照识别后会自动保存在这里")
                )
            }
        }
    }

    private func historyCard(_ item: RecognitionHistoryItem) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                historyImage(item.imageURL)
                    .frame(width: 92, height: 92)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                VStack(alignment: .leading, spacing: 5) {
                    Text(item.result.word)
                        .font(.system(.title2, design: .rounded, weight: .heavy))
                    Text(item.result.displayNameZh)
                        .foregroundStyle(FeltTheme.secondary)
                    Label(Self.timeFormatter.string(from: item.recognizedAt), systemImage: "clock")
                        .font(.caption)
                        .foregroundStyle(FeltTheme.secondary)
                }
                Spacer()
                Button { model.speech.speak(item.result.word) } label: {
                    Image(systemName: "speaker.wave.2.fill")
                        .foregroundStyle(FeltTheme.ink)
                        .frame(width: 42, height: 42)
                        .background(FeltTheme.yellow, in: Circle())
                }
            }

            HStack(spacing: 10) {
                Button { saveToWordbook(item) } label: {
                    Label(
                        model.isSavedToWordbook(item.result) ? "已在单词本" : "存入单词本",
                        systemImage: model.isSavedToWordbook(item.result)
                            ? "checkmark.circle.fill" : "plus.circle.fill"
                    )
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(FeltTheme.mint, in: RoundedRectangle(cornerRadius: 17))
                }
                .disabled(model.isSavedToWordbook(item.result))

                Button { generateStory(item) } label: {
                    Label("生成绘本", systemImage: "book.pages.fill")
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(FeltTheme.yellow, in: RoundedRectangle(cornerRadius: 17))
                }
            }
            .font(.system(.subheadline, design: .rounded, weight: .bold))
            .foregroundStyle(FeltTheme.ink)
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 24))
        .shadow(color: FeltTheme.ink.opacity(0.08), radius: 8, y: 4)
    }

    @ViewBuilder
    private func historyImage(_ url: URL?) -> some View {
        if let url {
            StoredImage(url: url).scaledToFill()
        } else {
            FeltObject(symbol: "camera.fill", color: FeltTheme.sky)
        }
    }

    private func saveToWordbook(_ item: RecognitionHistoryItem) {
        guard !model.isSavedToWordbook(item.result) else { return }
        model.save(word: item.result, imageURL: item.imageURL)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    private func generateStory(_ item: RecognitionHistoryItem) {
        let reference = item.imageURL.flatMap(Self.loadImage)
        model.selectedTab = .stories
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            model.startStoryGeneration(
                for: item.result,
                reference: reference,
                coverURL: item.imageURL
            )
        }
    }

    private static func loadImage(at url: URL) -> UIImage? {
        if let image = UIImage(contentsOfFile: url.path) { return image }
        guard let resolved = GeneratedImageStore.resolve(filename: url.lastPathComponent) else { return nil }
        return UIImage(contentsOfFile: resolved.path)
    }

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "M月d日 HH:mm"
        return formatter
    }()
}
