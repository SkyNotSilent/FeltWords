import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Hi, 小悠好～")
                            .font(.system(size: 30, weight: .bold, design: .rounded))
                        Text("今天也去发现一个新单词")
                            .foregroundStyle(FeltTheme.secondary)
                    }
                    Spacer()
                    FeltObject(symbol: "face.smiling", color: FeltTheme.yellow)
                        .frame(width: 72, height: 72)
                }

                FeltObject(symbol: "teddybear.fill", color: FeltTheme.orange)
                    .frame(height: 210)

                VStack(alignment: .leading, spacing: 14) {
                    Text("今日任务").font(.title3.bold())
                    Label("拍照找 1 个英文", systemImage: "camera.fill")
                    Label("听 3 次发音", systemImage: "speaker.wave.2.fill")
                    Label("看 1 本小绘本", systemImage: "book.fill")
                }
                .padding(22)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.white)
                .clipShape(RoundedRectangle(cornerRadius: 24))

                Button {
                    model.selectedTab = .camera
                } label: {
                    Label("开始拍照", systemImage: "camera.fill")
                }
                .buttonStyle(FeltButtonStyle(color: .white.opacity(0.7)))
            }
            .padding(24)
        }
        .background(FeltTheme.yellow)
        .foregroundStyle(FeltTheme.ink)
        .navigationBarHidden(true)
    }
}

