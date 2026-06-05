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

struct DailyTask: Codable, Identifiable, Hashable {
    let id: UUID
    let icon: String        // 前置 SF 图标
    let prefix: String      // 数字前文案
    var count: Int          // 唯一可编辑项
    let suffix: String      // 数字后文案

    init(id: UUID = UUID(), icon: String, prefix: String, count: Int, suffix: String) {
        self.id = id
        self.icon = icon
        self.prefix = prefix
        self.count = count
        self.suffix = suffix
    }
}
