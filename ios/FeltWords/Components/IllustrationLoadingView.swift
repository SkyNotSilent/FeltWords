import SwiftUI

/// 拍照后“识别 → 毛毡重绘 → 完成”三个阶段共用的对比视图：
/// 左边始终是孩子拍的原图，右边随阶段变化——先磨砂闪烁等待，最后淡入清晰的毛毡绘本。
/// 三个阶段保持同一套两格布局，因此切换时不会跳版，过渡只是右格内容的淡入淡出。
struct FeltComparisonView: View {
    let original: UIImage
    let stage: Stage

    enum Stage: Equatable {
        case recognizing          // 正在识别单词
        case generating           // 已识别，正在重绘毛毡
        case done(URL?)           // 完成；URL 为 nil 表示重绘失败，回退原图
    }

    private let tileHeight: CGFloat = 158

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
                    rightContent
                }
                .animation(.easeInOut(duration: 0.55), value: stage)
            }
        }
    }

    @ViewBuilder
    private var rightContent: some View {
        switch stage {
        case .recognizing, .generating:
            loadingTile(caption: stage == .recognizing ? "找单词中…" : "毛毡化中…")
                .transition(.opacity)
        case .done(let url):
            Group {
                if let url {
                    StoredImage(url: url).scaledToFill()
                } else {
                    Image(uiImage: original).resizable().scaledToFill()
                }
            }
            .transition(.opacity)
        }
    }

    private func loadingTile(caption: String) -> some View {
        ZStack {
            Image(uiImage: original).resizable().scaledToFill()
                .blur(radius: 14)
            Rectangle().fill(.ultraThinMaterial)
            ShimmerOverlay()
            VStack(spacing: 8) {
                ProgressView().tint(.white)
                Text(caption)
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .contentTransition(.opacity)
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
