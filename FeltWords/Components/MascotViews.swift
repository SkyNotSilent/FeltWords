import SwiftUI

struct MascotDailyStage: View {
    let recognizedCount: Int
    let wordCount: Int
    let storyCount: Int
    let onClose: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            BundledMascotImage(name: "mascot-key-art")
                .scaledToFill()
                .frame(height: 238)
                .clipped()

            LinearGradient(
                colors: [.clear, FeltTheme.surface.opacity(0.96)],
                startPoint: .center,
                endPoint: .bottom
            )

            VStack(spacing: 10) {
                Text("毛毛和朋友们今天也在等你")
                    .font(.system(.headline, design: .rounded, weight: .heavy))
                HStack(spacing: 8) {
                    statChip(value: recognizedCount, label: "发现")
                    statChip(value: wordCount, label: "单词")
                    statChip(value: storyCount, label: "绘本")
                }
            }
            .padding(.bottom, 14)
        }
        .overlay(alignment: .topTrailing) {
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .heavy))
                    .foregroundStyle(FeltTheme.ink)
                    .frame(width: 34, height: 34)
                    .feltGlassCircle(tint: FeltTheme.surface.opacity(0.2))
            }
            .buttonStyle(FeltPressStyle(pressedScale: 0.88))
            .padding(10)
            .accessibilityLabel("收起每日状态")
        }
        .frame(height: 238)
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .overlay(RoundedRectangle(cornerRadius: 28).stroke(.white.opacity(0.72), lineWidth: 2))
        .shadow(color: FeltTheme.ink.opacity(0.14), radius: 14, y: 7)
    }

    private func statChip(value: Int, label: String) -> some View {
        VStack(spacing: 1) {
            Text("\(value)").font(.system(.headline, design: .rounded, weight: .heavy))
            Text(label).font(.caption2.bold()).foregroundStyle(FeltTheme.secondary)
        }
        .frame(width: 62, height: 44)
        .feltGlassCapsule(tint: FeltTheme.surface.opacity(0.2))
    }
}

struct MascotPullCord: View {
    let action: () -> Void
    @GestureState private var pullOffset: CGFloat = 0

    var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(FeltTheme.orange.opacity(0.72))
                .frame(width: 3, height: 24 + min(max(pullOffset, 0), 48))
            ZStack {
                Circle().fill(FeltTheme.orange)
                Circle().stroke(.white, lineWidth: 3)
                Image(systemName: "pawprint.fill")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundStyle(.white)
            }
            .frame(width: 32, height: 32)
            Text("下拉看看毛毛")
                .font(.caption2.bold())
                .foregroundStyle(FeltTheme.secondary)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
        .onTapGesture(perform: action)
        .gesture(
            DragGesture(minimumDistance: 8)
                .updating($pullOffset) { value, state, _ in
                    state = max(0, value.translation.height)
                }
                .onEnded { value in
                    guard value.translation.height > 36 else { return }
                    action()
                }
        )
        .accessibilityLabel("展开每日状态")
        .accessibilityAddTraits(.isButton)
    }
}

struct MascotEmptyState: View {
    let title: String
    let description: String

    var body: some View {
        VStack(spacing: 14) {
            BundledMascotImage(name: "empty-state")
                .scaledToFill()
                .frame(width: 220, height: 170)
                .clipShape(RoundedRectangle(cornerRadius: 28))
                .overlay(RoundedRectangle(cornerRadius: 28).stroke(.white, lineWidth: 3))
                .shadow(color: FeltTheme.ink.opacity(0.12), radius: 10, y: 5)
            Text(title)
                .font(.system(.title3, design: .rounded, weight: .heavy))
            Text(description)
                .font(.subheadline)
                .foregroundStyle(FeltTheme.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
    }
}

struct BundledMascotImage: View {
    let name: String

    var body: some View {
        if let url = Bundle.main.url(forResource: name, withExtension: "png"),
           let image = UIImage(contentsOfFile: url.path) {
            Image(uiImage: image).resizable()
        } else {
            FeltObject(symbol: "teddybear.fill", color: FeltTheme.orange)
        }
    }
}
