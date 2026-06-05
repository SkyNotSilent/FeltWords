import Foundation

enum LocalStore {
    private static let wordsKey = "feltwords.words"
    private static let storiesKey = "feltwords.stories"
    private static let tasksKey = "feltwords.tasks"

    static func loadWords() -> [LearnedWord] {
        load([LearnedWord].self, key: wordsKey) ?? []
    }

    static func loadStories() -> [Storybook] {
        load([Storybook].self, key: storiesKey) ?? []
    }

    static func loadTasks() -> [DailyTask]? {
        load([DailyTask].self, key: tasksKey)
    }

    static func save(_ words: [LearnedWord]) {
        save(words, key: wordsKey)
    }

    static func save(_ stories: [Storybook]) {
        save(stories, key: storiesKey)
    }

    static func save(_ tasks: [DailyTask]) {
        save(tasks, key: tasksKey)
    }

    private static func load<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    private static func save<T: Encodable>(_ value: T, key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

