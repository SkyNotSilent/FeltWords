import SwiftUI

/// 主题色随昼夜（浅色/深色模式）自适应：白天暖黄绘本风，夜晚暖棕夜读风。
enum FeltTheme {
    static let yellow = Color(light: 0xFFD21F, dark: 0x241D10)   // 主背景
    static let orange = Color(light: 0xFF8A2A, dark: 0xF5963A)   // 强调/按钮
    static let cream = Color(light: 0xFFF6D8, dark: 0x322B1E)    // 暖色卡片
    static let mint = Color(light: 0xA9EBD6, dark: 0x2C5147)
    static let sky = Color(light: 0xBDEEFF, dark: 0x223038)
    static let pink = Color(light: 0xFFB8C8, dark: 0x5E3540)
    static let ink = Color(light: 0x3B2D1F, dark: 0xF6EAD0)      // 主文字
    static let secondary = Color(light: 0x8D7A56, dark: 0xB9A983)
    static let surface = Color(light: 0xFFFFFF, dark: 0x342C1F)  // 白卡片表面
}

extension Color {
    init(hex: UInt) {
        self.init(
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 8) & 0xff) / 255,
            blue: Double(hex & 0xff) / 255
        )
    }

    /// 随浅色/深色模式切换的动态颜色。
    init(light: UInt, dark: UInt) {
        self = Color(UIColor { trait in
            UIColor(hex: trait.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

extension UIColor {
    convenience init(hex: UInt) {
        self.init(
            red: CGFloat((hex >> 16) & 0xff) / 255,
            green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255,
            alpha: 1
        )
    }
}

struct FeltButtonStyle: ButtonStyle {
    let color: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(.headline, design: .rounded, weight: .bold))
            .foregroundStyle(FeltTheme.ink)
            .frame(maxWidth: .infinity, minHeight: 56)
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
    }
}

struct FeltPressStyle: ButtonStyle {
    var pressedScale: CGFloat = 0.97

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? pressedScale : 1)
            .brightness(configuration.isPressed ? 0.03 : 0)
            .animation(.spring(response: 0.28, dampingFraction: 0.72), value: configuration.isPressed)
    }
}

private struct FeltGlassCircleModifier: ViewModifier {
    let tint: Color

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.tint(tint).interactive(), in: .circle)
        } else {
            content
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.32), lineWidth: 1))
                .shadow(color: FeltTheme.ink.opacity(0.12), radius: 10, y: 4)
        }
    }
}

private struct FeltGlassCapsuleModifier: ViewModifier {
    let tint: Color

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.tint(tint).interactive(), in: .capsule)
        } else {
            content
                .background(.ultraThinMaterial, in: Capsule())
                .overlay(Capsule().stroke(.white.opacity(0.32), lineWidth: 1))
                .shadow(color: FeltTheme.ink.opacity(0.14), radius: 12, y: 5)
        }
    }
}

extension View {
    func feltGlassCircle(tint: Color = .clear) -> some View {
        modifier(FeltGlassCircleModifier(tint: tint))
    }

    func feltGlassCapsule(tint: Color = .clear) -> some View {
        modifier(FeltGlassCapsuleModifier(tint: tint))
    }
}

struct FeltObject: View {
    let symbol: String
    var color: Color = FeltTheme.pink

    var body: some View {
        ZStack {
            Circle().fill(color.opacity(0.35))
            Circle().stroke(color, style: StrokeStyle(lineWidth: 4, dash: [3, 2]))
            Image(systemName: symbol)
                .font(.system(size: 46, weight: .semibold))
                .foregroundStyle(FeltTheme.ink)
        }
        .padding(8)
        .background(FeltTheme.surface.opacity(0.65))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}
