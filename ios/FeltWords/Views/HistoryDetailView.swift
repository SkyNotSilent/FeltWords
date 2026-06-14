import SwiftUI

struct HistoryDetailView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let item: RecognitionHistoryItem

    private var result: RecognitionResult { item.result }
    private var isSaved: Bool { model.isSavedToWordbook(result) }
    private var displayURL: URL? { item.imageURL ?? item.capturedImagePath }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                coverImage
                wordSection
                sentenceCard
                alternativesRow
                actionButtons
            }
            .padding(24)
        }
        .background(FeltTheme.cream)
        .foregroundStyle(FeltTheme.ink)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button { dismiss() } label: {
                    Image(systemName: "chevron.left")
                        .font(.headline.bold())
                        .foregroundStyle(FeltTheme.ink)
                        .frame(width: 36, height: 36)
                        .background(FeltTheme.surface.opacity(0.7), in: Circle())
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .onDisappear { model.speech.stop() }
    }

    @ViewBuilder
    private var coverImage: some View {
        if let url = displayURL {
            StoredImage(url: url)
                .scaledToFill()
                .frame(maxWidth: .infinity)
                .frame(height: 220)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .shadow(color: FeltTheme.ink.opacity(0.12), radius: 10, y: 5)
        }
    }

    private var wordSection: some View {
        VStack(spacing: 6) {
            HStack(spacing: 12) {
                Text(result.word)
                    .font(.system(size: 48, weight: .heavy, design: .rounded))

                Button { model.speech.speak(result.word) } label: {
                    AnimatedSpeakerView(
                        isSpeaking: model.speech.isSpeaking,
                        tint: FeltTheme.ink
                    )
                    .frame(width: 48, height: 48)
                    .background(FeltTheme.yellow, in: Circle())
                }
            }
            Text(result.displayNameZh)
                .font(.title3)
                .foregroundStyle(FeltTheme.secondary)
        }
    }

    private var sentenceCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(result.exampleSentence)
                .font(.system(.title3, design: .rounded, weight: .bold))
                .frame(maxWidth: .infinity, alignment: .leading)

            if !result.exampleSentenceZh.isEmpty {
                Text(result.exampleSentenceZh)
                    .font(.body)
                    .foregroundStyle(FeltTheme.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(22)
        .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 22))
        .onTapGesture {
            model.speech.speak("\(result.word). \(result.exampleSentence)")
        }
    }

    @ViewBuilder
    private var alternativesRow: some View {
        if !result.alternatives.isEmpty {
            HStack(spacing: 8) {
                ForEach(result.alternatives.prefix(3), id: \.self) { alt in
                    Text(alt)
                        .font(.body)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(FeltTheme.surface, in: Capsule())
                }
            }
        }
    }

    private var actionButtons: some View {
        VStack(spacing: 12) {
            Button { generateStory() } label: {
                Label("生成小绘本", systemImage: "book.pages.fill")
            }
            .buttonStyle(FeltButtonStyle(color: FeltTheme.yellow))

            Button { saveWord() } label: {
                Label(
                    isSaved ? "已加入单词本" : "加入单词本",
                    systemImage: isSaved ? "checkmark.circle.fill" : "plus.circle.fill"
                )
            }
            .buttonStyle(FeltButtonStyle(color: isSaved ? FeltTheme.mint : FeltTheme.surface))
            .disabled(isSaved)
        }
    }

    private func saveWord() {
        guard !isSaved else { return }
        model.save(word: result, imageURL: item.imageURL)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    private func generateStory() {
        let reference = (item.imageURL ?? item.capturedImagePath)
            .flatMap { UIImage(contentsOfFile: $0.path) }
        model.speech.stop()
        model.selectedTab = .stories
        dismiss()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            model.startStoryGeneration(
                for: result,
                reference: reference,
                coverURL: item.imageURL
            )
        }
    }
}
