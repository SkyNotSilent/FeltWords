import SwiftUI

enum FeltTheme {
    static let yellow = Color(hex: 0xFFD21F)
    static let orange = Color(hex: 0xFF8A2A)
    static let cream = Color(hex: 0xFFF6D8)
    static let mint = Color(hex: 0xA9EBD6)
    static let sky = Color(hex: 0xBDEEFF)
    static let pink = Color(hex: 0xFFB8C8)
    static let ink = Color(hex: 0x3B2D1F)
    static let secondary = Color(hex: 0x8D7A56)
}

extension Color {
    init(hex: UInt) {
        self.init(
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 8) & 0xff) / 255,
            blue: Double(hex & 0xff) / 255
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
        .background(.white.opacity(0.65))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

