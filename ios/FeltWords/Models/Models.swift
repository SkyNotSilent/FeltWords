import Foundation

struct RecognitionResult: Codable, Hashable, Identifiable {
    let id: UUID
    let word: String
    let displayNameZh: String
    let confidence: Double
    let category: String
    let childFriendlyDefinition: String
    let exampleSentence: String
    let exampleSentenceZh: String
    let visualDescription: String
    let alternatives: [String]

    init(
        id: UUID = UUID(),
        word: String,
        displayNameZh: String,
        confidence: Double,
        category: String,
        childFriendlyDefinition: String,
        exampleSentence: String,
        exampleSentenceZh: String = "",
        visualDescription: String,
        alternatives: [String]
    ) {
        self.id = id
        self.word = word
        self.displayNameZh = displayNameZh
        self.confidence = confidence
        self.category = category
        self.childFriendlyDefinition = childFriendlyDefinition
        self.exampleSentence = exampleSentence
        self.exampleSentenceZh = exampleSentenceZh
        self.visualDescription = visualDescription
        self.alternatives = alternatives
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = (try? c.decode(UUID.self, forKey: .id)) ?? UUID()
        word = try c.decode(String.self, forKey: .word)
        displayNameZh = try c.decode(String.self, forKey: .displayNameZh)
        confidence = try c.decode(Double.self, forKey: .confidence)
        category = try c.decode(String.self, forKey: .category)
        childFriendlyDefinition = try c.decode(String.self, forKey: .childFriendlyDefinition)
        exampleSentence = try c.decode(String.self, forKey: .exampleSentence)
        exampleSentenceZh = (try? c.decode(String.self, forKey: .exampleSentenceZh)) ?? ""
        visualDescription = try c.decode(String.self, forKey: .visualDescription)
        alternatives = try c.decode([String].self, forKey: .alternatives)
    }
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

struct RecognitionHistoryItem: Codable, Identifiable, Hashable {
    let id: UUID
    let result: RecognitionResult
    let imageURL: URL?
    let capturedImagePath: URL?
    let recognizedAt: Date

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        result = try c.decode(RecognitionResult.self, forKey: .result)
        imageURL = try c.decodeIfPresent(URL.self, forKey: .imageURL)
        capturedImagePath = try? c.decodeIfPresent(URL.self, forKey: .capturedImagePath)
        recognizedAt = try c.decode(Date.self, forKey: .recognizedAt)
    }

    init(id: UUID = UUID(), result: RecognitionResult, imageURL: URL?, capturedImagePath: URL? = nil, recognizedAt: Date = Date()) {
        self.id = id
        self.result = result
        self.imageURL = imageURL
        self.capturedImagePath = capturedImagePath
        self.recognizedAt = recognizedAt
    }
}

struct StoryPage: Codable, Identifiable, Hashable {
    let id: UUID
    let sentence: String
    let sentenceZh: String
    let imageURL: URL?

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        sentence = try c.decode(String.self, forKey: .sentence)
        sentenceZh = (try? c.decode(String.self, forKey: .sentenceZh)) ?? ""
        imageURL = try c.decodeIfPresent(URL.self, forKey: .imageURL)
    }

    init(id: UUID = UUID(), sentence: String, sentenceZh: String = "", imageURL: URL? = nil) {
        self.id = id
        self.sentence = sentence
        self.sentenceZh = sentenceZh
        self.imageURL = imageURL
    }
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
