import SwiftUI

struct WordResultView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let originalImage: UIImage

    @State private var result: RecognitionResult?
    @State private var generatedImageURL: URL?
    @State private var isGeneratingIllustration = false
    @State private var recognitionError: String?

    @State private var isSaved = false

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
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button { exitToHome() } label: {
                    Image(systemName: "xmark")
                        .font(.headline.bold())
                        .foregroundStyle(FeltTheme.ink)
                        .frame(width: 36, height: 36)
                        .background(FeltTheme.surface, in: Circle())
                }
            }
        }
        .task { await runRecognitionAndIllustration() }
    }

    private func exitToHome() {
        model.speech.stop()
        model.selectedTab = .home
        dismiss()
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
                AnimatedSpeakerView(
                    isSpeaking: model.speech.isSpeaking,
                    tint: FeltTheme.ink
                )
                .frame(width: 48, height: 48)
                .background(FeltTheme.yellow, in: Circle())
            }
        }
        Text(result.displayNameZh).font(.headline).foregroundStyle(FeltTheme.secondary)
        Label("已自动存入历史记录", systemImage: "clock.badge.checkmark")
            .font(.system(.subheadline, design: .rounded, weight: .bold))
            .foregroundStyle(FeltTheme.secondary)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(FeltTheme.surface, in: Capsule())
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
        .onTapGesture { model.speech.speak("\(result.word). \(result.exampleSentence)") }

        if !result.alternatives.isEmpty {
            HStack {
                ForEach(result.alternatives.prefix(3), id: \.self) { word in
                    Text(word).padding(.horizontal, 14).padding(.vertical, 8)
                        .background(FeltTheme.surface, in: Capsule())
                }
            }
        }

        Button { startStoryGeneration(for: result) } label: {
            Label("生成小绘本", systemImage: "book.pages.fill")
        }
        .buttonStyle(FeltButtonStyle(color: FeltTheme.yellow))
        .disabled(isGeneratingIllustration)

        Button {
            guard !isSaved else { return }
            model.save(word: result, imageURL: generatedImageURL)
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) { isSaved = true }
        } label: {
            Label(isSaved ? "已加入单词本" : "加入单词本",
                  systemImage: isSaved ? "checkmark.circle.fill" : "plus.circle.fill")
        }
        .buttonStyle(FeltButtonStyle(color: isSaved ? FeltTheme.mint : FeltTheme.surface))
        .disabled(isSaved)
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
        .background(FeltTheme.surface, in: RoundedRectangle(cornerRadius: 24))
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
            let capturedURL = try? GeneratedImageStore.persist(image: originalImage)
            let savedHistoryID = model.save(history: recognized, imageURL: nil, capturedImagePath: capturedURL)
            withAnimation(.easeInOut(duration: 0.5)) {
                result = recognized
                isGeneratingIllustration = true
            }

            // 毛毡插图是增强项，失败时静默回退到原始照片，不打断阅读体验。
            let url = try? await model.agnes.generateFeltImage(for: recognized, sourceImage: originalImage)
            if let url {
                model.updateHistoryImage(id: savedHistoryID, imageURL: url)
            }
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

    /// 后台生成绘本：立即切到绘本页，让"生成中"卡片弹出，用户无需在此等待。
    private func startStoryGeneration(for result: RecognitionResult) {
        // 用已生成的毛毡图作为每页连环画的参考图，保持画面连续；没有就退回原始照片。
        let reference = generatedImageURL
            .flatMap { UIImage(contentsOfFile: $0.path) } ?? originalImage
        let coverURL = generatedImageURL
        model.speech.stop()
        model.selectedTab = .stories
        dismiss()
        // 等切到绘本页后再插入，卡片"弹出"动画才看得见。
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            model.startStoryGeneration(for: result, reference: reference, coverURL: coverURL)
        }
    }
}
