import SwiftUI

struct MascotDailyTheme {
    let assetName: String
    let title: String
    let subtitle: String

    static let all = [
        MascotDailyTheme(
            assetName: "mascot-key-art",
            title: "毛毛和朋友们今天也在等你",
            subtitle: "一起发现身边的新单词"
        ),
        MascotDailyTheme(
            assetName: "daily-eating",
            title: "认真吃饭，身体有力量",
            subtitle: "今天也要尝一口蔬菜"
        ),
        MascotDailyTheme(
            assetName: "daily-learning",
            title: "每天认识一个新单词",
            subtitle: "小小进步也很了不起"
        ),
        MascotDailyTheme(
            assetName: "daily-playing",
            title: "一起玩，也要互相照顾",
            subtitle: "分享会让快乐变更多"
        ),
        MascotDailyTheme(
            assetName: "daily-tidying",
            title: "玩具回家，房间更舒服",
            subtitle: "我们一起收拾吧"
        ),
        MascotDailyTheme(
            assetName: "daily-bedtime",
            title: "早点睡觉，明天更有精神",
            subtitle: "毛毛和你说晚安"
        )
    ]

    /// 每次 App 完整启动前进一个主题；首次启动显示默认母图。
    static func nextLaunch(defaults: UserDefaults = .standard) -> MascotDailyTheme {
        let key = "feltwords.mascotDailyThemeIndex"
        let index = defaults.object(forKey: key) == nil
            ? 0
            : defaults.integer(forKey: key) % all.count
        defaults.set((index + 1) % all.count, forKey: key)
        return all[index]
    }
}

struct MascotDailyStage: View {
    let theme: MascotDailyTheme
    let recognizedCount: Int
    let wordCount: Int
    let storyCount: Int

    var body: some View {
        ZStack(alignment: .bottom) {
            BundledMascotImage(name: theme.assetName)
                .scaledToFill()
                .frame(height: 238)
                .clipped()

            LinearGradient(
                colors: [.clear, FeltTheme.surface.opacity(0.96)],
                startPoint: .center,
                endPoint: .bottom
            )

            VStack(spacing: 7) {
                Text(theme.title)
                    .font(.system(.headline, design: .rounded, weight: .heavy))
                Text(theme.subtitle)
                    .font(.caption.bold())
                    .foregroundStyle(FeltTheme.secondary)
                HStack(spacing: 8) {
                    statChip(value: recognizedCount, label: "发现")
                    statChip(value: wordCount, label: "单词")
                    statChip(value: storyCount, label: "绘本")
                }
            }
            .padding(.bottom, 14)
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

/// 纯展示组件：垂直拉绳随 `stretch` 实时伸长，手势与回弹由父视图控制。
struct MascotPullCord: View {
    var stretch: CGFloat = 0

    var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(FeltTheme.orange.opacity(0.72))
                .frame(width: 3, height: 24 + min(max(stretch, 0), 48))
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
        .fixedSize()
        .accessibilityLabel("下拉查看每日状态")
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
