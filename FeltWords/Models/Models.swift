import Foundation

struct RecognitionResult: Codable, Hashable, Identifiable {
    var id: String { "\(word)-\(displayNameZh)" }
    let word: String
    let displayNameZh: String
    let confidence: Double
    let category: String
    let childFriendlyDefinition: String
    let exampleSentence: String
    let visualDescription: String
    let alternatives: [String]
}

struct LearnedWord: Codable, Identifiable, Hashable {
    let id: UUID
    let word: String
    let displayNameZh: String
    let exampleSentence: String
    let category: String
    let imageURL: URL?
    let learnedAt: Date
}

struct StoryPage: Codable, Identifiable, Hashable {
    let id: UUID
    let sentence: String
    let imageURL: URL?
}

struct Storybook: Codable, Identifiable, Hashable {
    let id: UUID
    let title: String
    let focusWord: String
    let createdAt: Date
    let pages: [StoryPage]
}
