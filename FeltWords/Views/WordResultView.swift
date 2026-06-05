import SwiftUI

struct WordResultView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let originalImage: UIImage

    @State private var result: RecognitionResult?
    @State private var generatedImageURL: URL?
    @State private var isGeneratingIllustration = false
    @State private var recognitionError: String?

    @State private var isGeneratingStory = false
    @State private var storyError: String?
    @State private var storyRoute: Storybook?

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if let recognitionError {
                    failedCard(message: recognitionError)
                } else {
                    FeltComparisonView(original: originalImage, stage: stage)
                        .frame(maxWidth: .infinity)

                    if let result {
                        resultDetails(result)
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                    } else {
                        Text("毛毛正在认真看…")
                            .font(.headline)
                            .foregroundStyle(FeltTheme.secondary)
                            .transition(.opacity)
                    }
                }
            }
            .padding(24)
        }
        .background(FeltTheme.cream)
        .foregroundStyle(FeltTheme.ink)
        .navigationDestination(item: $storyRoute) { StoryReaderView(story: $0) }
        .alert("毛毛还在想", isPresented: .constant(storyError != nil)) {
            Button("好的") { storyError = nil }
        } message: { Text(storyError ?? "") }
        .task { await runRecognitionAndIllustration() }
    }

    /// 当前对比卡片应处的阶段：识别中 → 重绘中 → 完成。
    private var stage: FeltComparisonView.Stage {
        if result == nil { return .recognizing }
        if isGeneratingIllustration { return .generating }
        return .done(generatedImageURL)
    }

    @ViewBuilder
    private func resultDetails(_ result: RecognitionResult) -> some View {
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

        Button { generateStory(for: result) } label: {
            Label(isGeneratingStory ? "正在生成…" : "生成小绘本", systemImage: "book.pages.fill")
        }
        .buttonStyle(FeltButtonStyle(color: FeltTheme.yellow))
        .disabled(isGeneratingStory || isGeneratingIllustration)

        Button {
            model.save(word: result, imageURL: generatedImageURL)
        } label: {
            Label("加入单词本", systemImage: "plus.circle.fill")
        }
        .buttonStyle(FeltButtonStyle(color: .white))
    }

    @ViewBuilder
    private func failedCard(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "eye.slash.fill").font(.largeTitle).foregroundStyle(FeltTheme.orange)
            Text(message)
                .font(.headline)
                .multilineTextAlignment(.center)
            Button("重新拍照") { dismiss() }
                .buttonStyle(FeltButtonStyle(color: FeltTheme.yellow))
        }
        .padding(28)
        .frame(maxWidth: .infinity)
        .background(.white, in: RoundedRectangle(cornerRadius: 24))
        .padding(.top, 40)
    }

    /// 落地即开始：先识别单词，紧接着重绘毛毡插图，全程由对比卡片连续呈现。
    private func runRecognitionAndIllustration() async {
        guard result == nil, recognitionError == nil else { return }
        do {
            if try await PhotoSafetyService.containsFace(in: originalImage) {
                throw AgnesError.server("照片里好像有人，请只拍物品再试一次。")
            }
            let recognized = try await model.agnes.recognize(image: originalImage)
            model.latestResult = recognized
            withAnimation(.easeInOut(duration: 0.5)) {
                result = recognized
                isGeneratingIllustration = true
            }

            // 毛毡插图是增强项，失败时静默回退到原始照片，不打断阅读体验。
            let url = try? await model.agnes.generateFeltImage(for: recognized, sourceImage: originalImage)
            withAnimation(.easeInOut(duration: 0.6)) {
                generatedImageURL = url
                isGeneratingIllustration = false
            }
        } catch let error as AgnesError {
            withAnimation(.easeInOut(duration: 0.4)) { recognitionError = error.localizedDescription }
        } catch {
            // 系统底层错误（如 Vision 推理失败）不直接把英文抛给孩子，统一用友好中文兜底。
            withAnimation(.easeInOut(duration: 0.4)) { recognitionError = "毛毛刚才没看清楚，请再试一次吧。" }
        }
    }

    private func generateStory(for result: RecognitionResult) {
        isGeneratingStory = true
        Task {
            do {
                let story = try await model.agnes.generateStory(for: result, imageURL: generatedImageURL)
                model.save(story: story)
                storyRoute = story
            } catch {
                storyError = error.localizedDescription
            }
            isGeneratingStory = false
        }
    }
}
