import SwiftUI

@MainActor
final class AppModel: ObservableObject {
    @Published var selectedTab: AppTab = .home
    @Published var words: [LearnedWord] = LocalStore.loadWords()
    @Published var stories: [Storybook] = LocalStore.loadStories()
    @Published var latestResult: RecognitionResult?
    @Published var capturedImage: UIImage?

    let agnes = AgnesAPIService()
    let speech = SpeechService()

    func save(word result: RecognitionResult, imageURL: URL?) {
        let word = LearnedWord(
            id: UUID(),
            word: result.word,
            displayNameZh: result.displayNameZh,
            exampleSentence: result.exampleSentence,
            category: result.category,
            imageURL: imageURL,
            learnedAt: .now
        )
        words.removeAll { $0.word.lowercased() == word.word.lowercased() }
        words.insert(word, at: 0)
        LocalStore.save(words)
    }

    func save(story: Storybook) {
        stories.insert(story, at: 0)
        LocalStore.save(stories)
    }
}

enum AppTab: Hashable {
    case home
    case camera
    case stories
    case words
}

