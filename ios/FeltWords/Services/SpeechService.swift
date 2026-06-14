import AVFoundation

@MainActor
final class SpeechService: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    private let synthesizer = AVSpeechSynthesizer()
    private let childFriendlyRate: Float = 0.36
    @Published private(set) var isSpeaking = false

    /// 当前朗读自然结束时的回调。被 stop() 或新朗读打断时不会触发——
    /// 这样自动播放只在"这页读完了"时才翻页，手动暂停不会误翻。
    private var onFinish: (() -> Void)?

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func speak(_ text: String, onFinish: (() -> Void)? = nil) {
        synthesizer.stopSpeaking(at: .immediate)
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            isSpeaking = false
            return
        }
        self.onFinish = onFinish
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        utterance.rate = childFriendlyRate
        utterance.pitchMultiplier = 1.08
        isSpeaking = true
        synthesizer.speak(utterance)
    }

    func stop() {
        onFinish = nil
        synthesizer.stopSpeaking(at: .immediate)
        isSpeaking = false
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer,
                                       didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in
            isSpeaking = false
            let finish = onFinish
            onFinish = nil
            finish?()
        }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer,
                                       didCancel utterance: AVSpeechUtterance) {
        Task { @MainActor in
            isSpeaking = false
        }
    }
}
