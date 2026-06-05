import SwiftUI

struct StoryLibraryView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: 18) {
                ForEach(model.stories) { story in
                    NavigationLink(value: story) {
                        VStack(alignment: .leading, spacing: 10) {
                            StoryImage(url: story.pages.first?.imageURL)
                                .frame(height: 130)
                            Text(story.title).font(.headline).lineLimit(2)
                            Text(story.focusWord).font(.caption).foregroundStyle(FeltTheme.secondary)
                        }
                        .padding(12)
                        .background(.white, in: RoundedRectangle(cornerRadius: 20))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(20)
        }
        .background(FeltTheme.cream)
        .navigationTitle("我的绘本")
        .navigationDestination(for: Storybook.self) { StoryReaderView(story: $0) }
        .overlay {
            if model.stories.isEmpty {
                ContentUnavailableView("还没有绘本", systemImage: "book.closed", description: Text("拍一个物品，生成第一本小故事"))
            }
        }
    }
}

struct StoryReaderView: View {
    @EnvironmentObject private var model: AppModel
    let story: Storybook
    @State private var page = 0

    var body: some View {
        VStack(spacing: 24) {
            StoryImage(url: story.pages[page].imageURL)
                .frame(maxWidth: .infinity, maxHeight: 430)
            Text(story.pages[page].sentence)
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            HStack {
                Button { page = max(0, page - 1) } label: { Image(systemName: "chevron.left") }
                    .disabled(page == 0)
                Spacer()
                Button { model.speech.speak(story.pages[page].sentence) } label: {
                    Image(systemName: "play.fill").frame(width: 64, height: 64).background(FeltTheme.yellow, in: Circle())
                }
                Spacer()
                Button { page = min(story.pages.count - 1, page + 1) } label: { Image(systemName: "chevron.right") }
                    .disabled(page == story.pages.count - 1)
            }
            .font(.title2.bold())
            .padding(.horizontal, 44)
        }
        .padding(20)
        .background(FeltTheme.sky.ignoresSafeArea())
        .foregroundStyle(FeltTheme.ink)
        .navigationTitle(story.title)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: page) { _, _ in model.speech.speak(story.pages[page].sentence) }
        .onAppear { model.speech.speak(story.pages[page].sentence) }
    }
}

private struct StoryImage: View {
    let url: URL?

    var body: some View {
        Group {
            if let url {
                AsyncImage(url: url) { image in image.resizable().scaledToFill() } placeholder: { ProgressView() }
            } else {
                FeltObject(symbol: "book.pages.fill", color: FeltTheme.mint)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }
}

