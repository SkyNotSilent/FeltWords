import SwiftUI

/// 毛毡插图生成中的等待态：左侧是孩子拍的原图，右侧用同一张图做磨砂模糊 + 闪烁高光，
/// 营造“正在把照片变成毛毡绘本”的过程感。生成完成后由父视图淡入清晰结果。
struct IllustrationLoadingView: View {
    let original: UIImage

    private let tileHeight: CGFloat = 150

    var body: some View {
        HStack(spacing: 12) {
            tile(label: "你的照片") {
                Image(uiImage: original).resizable().scaledToFill()
            }

            Image(systemName: "arrow.right")
                .font(.title3.bold())
                .foregroundStyle(FeltTheme.orange)
                .symbolEffect(.pulse, options: .repeating)

            tile(label: "毛毡绘本") {
                ZStack {
                    Image(uiImage: original).resizable().scaledToFill()
                        .blur(radius: 14)
                    Rectangle().fill(.ultraThinMaterial)
                    ShimmerOverlay()
                    VStack(spacing: 8) {
                        ProgressView().tint(.white)
                        Text("毛毡化中…")
                            .font(.caption.bold())
                            .foregroundStyle(.white)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func tile<Content: View>(label: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 6) {
            content()
                .frame(maxWidth: .infinity)
                .frame(height: tileHeight)
                .clipShape(RoundedRectangle(cornerRadius: 18))
            Text(label)
                .font(.caption)
                .foregroundStyle(FeltTheme.secondary)
        }
    }
}

/// 一条斜向高光循环扫过，制造闪烁等待感。
private struct ShimmerOverlay: View {
    @State private var phase: CGFloat = -1

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            LinearGradient(
                colors: [.clear, .white.opacity(0.6), .clear],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .frame(width: width * 1.4)
            .offset(x: phase * width * 1.6)
            .blendMode(.plusLighter)
            .onAppear {
                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                    phase = 1
                }
            }
        }
        .allowsHitTesting(false)
    }
}
