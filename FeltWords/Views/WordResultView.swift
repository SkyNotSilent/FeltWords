import SwiftUI

struct WordResultView: View {
    @EnvironmentObject private var model: AppModel
    let result: RecognitionResult
    let originalImage: UIImage?

    @State private var generatedImageURL: URL?
    @State private var isGenerating = false
    @State private var errorMessage: String?
    @State private var storyRoute: Storybook?

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                imageCard
                Text("找到啦，是").foregroundStyle(FeltTheme.secondary)
                HStack {
                    Text(result.word)
                        .font(.system(size: 52, weight: .heavy, design: .rounded))
                    Button { model.speech.speak(result.word) } label: {
                        Image(systemName: "speaker.wave.2.fill")
                            .frame(width: 48, height: 48)
                            .background(FeltTheme.yellow, in: Circle())
                    }
                }
                Text(result.displayNameZh).font(.headline).foregroundStyle(FeltTheme.secondary)
                Text(result.exampleSentence)
                    .font(.system(.title3, design: .rounded, weight: .bold))
                    .frame(maxWidth: .infinity)
                    .padding(22)
                    .background(.white, in: RoundedRectangle(cornerRadius: 22))

                if !result.alternatives.isEmpty {
                    HStack {
                        ForEach(result.alternatives.prefix(3), id: \.self) { word in
                            Text(word).padding(.horizontal, 14).padding(.vertical, 8)
                                .background(.white, in: Capsule())
                        }
                    }
                }

                Button { generateStory() } label: {
                    Label(isGenerating ? "正在生成…" : "生成小绘本", systemImage: "book.pages.fill")
                }
                .buttonStyle(FeltButtonStyle(color: FeltTheme.yellow))
                .disabled(isGenerating)

                Button {
                    model.save(word: result, imageURL: generatedImageURL)
                } label: {
                    Label("加入单词本", systemImage: "plus.circle.fill")
                }
                .buttonStyle(FeltButtonStyle(color: .white))
            }
            .padding(24)
        }
        .background(FeltTheme.cream)
        .foregroundStyle(FeltTheme.ink)
        .navigationDestination(item: $storyRoute) { StoryReaderView(story: $0) }
        .alert("毛毛还在想", isPresented: .constant(errorMessage != nil)) {
            Button("好的") { errorMessage = nil }
        } message: { Text(errorMessage ?? "") }
        .task { await generateIllustration() }
    }

    @ViewBuilder
    private var imageCard: some View {
        Group {
            if let generatedImageURL {
                StoredImage(url: generatedImageURL).scaledToFill()
            } else if let originalImage {
                Image(uiImage: originalImage).resizable().scaledToFill()
            } else {
                FeltObject(symbol: "sparkles", color: FeltTheme.sky)
            }
        }
        .frame(height: 220)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 26))
    }

    private func generateIllustration() async {
        do {
            generatedImageURL = try await model.agnes.generateFeltImage(for: result, sourceImage: originalImage)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func generateStory() {
        isGenerating = true
        Task {
            do {
                let story = try await model.agnes.generateStory(for: result, imageURL: generatedImageURL)
                model.save(story: story)
                storyRoute = story
            } catch {
                errorMessage = error.localizedDescription
            }
            isGenerating = false
        }
    }
}
