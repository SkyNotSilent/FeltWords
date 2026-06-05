import SwiftUI

struct WordbookView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        List(model.words) { word in
            Button {
                model.speech.speak(word.word)
            } label: {
                HStack(spacing: 16) {
                    FeltObject(symbol: "textformat.abc", color: FeltTheme.mint)
                        .frame(width: 72, height: 72)
                    VStack(alignment: .leading, spacing: 5) {
                        Text(word.word).font(.system(.title2, design: .rounded, weight: .bold))
                        Text(word.displayNameZh).foregroundStyle(FeltTheme.secondary)
                        Text(word.exampleSentence).font(.caption).foregroundStyle(FeltTheme.secondary)
                    }
                    Spacer()
                    Image(systemName: "speaker.wave.2.fill").foregroundStyle(FeltTheme.orange)
                }
            }
            .buttonStyle(.plain)
            .listRowBackground(FeltTheme.surface)
        }
        .scrollContentBackground(.hidden)
        .background(FeltTheme.cream)
        .navigationTitle("单词本")
        .overlay {
            if model.words.isEmpty {
                ContentUnavailableView("还没有单词", systemImage: "textformat.abc", description: Text("识别物品后，把单词收藏在这里"))
            }
        }
    }
}

