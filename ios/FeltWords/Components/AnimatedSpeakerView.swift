import SwiftUI

struct AnimatedSpeakerView: View {
    let isSpeaking: Bool
    var tint: Color = FeltTheme.ink
    var size: CGFloat = 24

    var body: some View {
        Image(systemName: "speaker.wave.3.fill")
            .font(.system(size: size * 0.7, weight: .medium))
            .foregroundStyle(tint)
            .symbolEffect(.variableColor.iterative, isActive: isSpeaking)
            .contentTransition(.symbolEffect(.replace))
            .frame(width: size, height: size)
    }
}
