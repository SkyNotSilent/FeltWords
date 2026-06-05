import SwiftUI

@MainActor
final class AppModel: ObservableObject {
    @Published var selectedTab: AppTab = .home
    @Published var words: [LearnedWord] = LocalStore.loadWords()
    @Published var stories: [Storybook] = LocalStore.loadStories()
    @Published var history: [RecognitionHistoryItem] = LocalStore.loadHistory()
    /// 正在后台生成的绘本，绘本页用它显示"生成中"占位卡片（最新在前）。
    @Published var generatingStories: [GeneratingStory] = []
    @Published var latestResult: RecognitionResult?
    @Published var capturedImage: UIImage?

    @Published var tasks: [DailyTask] = LocalStore.loadTasks() ?? AppModel.defaultTasks
    @Published var avatarImage: UIImage? = ProfileStore.loadAvatar()
    /// 正在为缺图老单词补毛毡图的 ID 集合，单词本用它显示加载态。
    @Published var backfillingWordIDs: Set<UUID> = []

    let agnes = AgnesAPIService()
    let speech = SpeechService()

    static let defaultTasks = [
        DailyTask(icon: "camera.fill", prefix: "拍照找 ", count: 1, suffix: " 个英文"),
        DailyTask(icon: "speaker.wave.2.fill", prefix: "听 ", count: 3, suffix: " 次发音"),
        DailyTask(icon: "book.fill", prefix: "看 ", count: 1, suffix: " 本小绘本")
    ]

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

    @discardableResult
    func save(history result: RecognitionResult, imageURL: URL?) -> UUID {
        let item = RecognitionHistoryItem(
            id: UUID(),
            result: result,
            imageURL: imageURL,
            recognizedAt: .now
        )
        history.insert(item, at: 0)
        LocalStore.save(history)
        return item.id
    }

    func updateHistoryImage(id: UUID, imageURL: URL?) {
        guard let index = history.firstIndex(where: { $0.id == id }) else { return }
        let item = history[index]
        history[index] = RecognitionHistoryItem(
            id: item.id,
            result: item.result,
            imageURL: imageURL,
            recognizedAt: item.recognizedAt
        )
        LocalStore.save(history)
    }

    func isSavedToWordbook(_ result: RecognitionResult) -> Bool {
        words.contains { $0.word.caseInsensitiveCompare(result.word) == .orderedSame }
    }

    func deleteWord(id: UUID) {
        words.removeAll { $0.id == id }
        LocalStore.save(words)
    }

    func restoreWords(_ restored: [(index: Int, word: LearnedWord)]) {
        for item in restored.sorted(by: { $0.index < $1.index }) {
            words.removeAll { $0.id == item.word.id }
            words.insert(item.word, at: min(item.index, words.count))
        }
        LocalStore.save(words)
    }

    func deleteStory(id: UUID) {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.72)) {
            stories.removeAll { $0.id == id }
        }
        LocalStore.save(stories)
    }

    /// 一次性把缺图老单词，从同名绘本里借一张现成的毛毡图（本地文件，零 API、即时）。
    /// 返回是否有改动；单词本出现时调用即可自动补好大部分老数据。
    @discardableResult
    func linkStoryImagesToWords() -> Bool {
        var changed = false
        words = words.map { word in
            guard word.imageURL == nil,
                  let story = stories.first(where: { $0.focusWord.lowercased() == word.word.lowercased() }),
                  let pageURL = story.pages.first(where: { $0.imageURL != nil })?.imageURL
            else { return word }
            changed = true
            return LearnedWord(
                id: word.id, word: word.word, displayNameZh: word.displayNameZh,
                exampleSentence: word.exampleSentence, category: word.category,
                imageURL: pageURL, learnedAt: word.learnedAt
            )
        }
        if changed { LocalStore.save(words) }
        return changed
    }

    /// 为没有插图、也没有同名绘本的老单词文生图补一张毛毡图（按单词+释义重画），成功后持久化。
    func backfillImage(for id: UUID) {
        guard let word = words.first(where: { $0.id == id }),
              word.imageURL == nil,
              !backfillingWordIDs.contains(id) else { return }
        backfillingWordIDs.insert(id)
        let result = RecognitionResult(
            word: word.word,
            displayNameZh: word.displayNameZh,
            confidence: 1,
            category: word.category,
            childFriendlyDefinition: "",
            exampleSentence: word.exampleSentence,
            visualDescription: "\(word.word)（\(word.displayNameZh)）",
            alternatives: []
        )
        Task {
            defer { backfillingWordIDs.remove(id) }
            guard let url = try? await agnes.generateFeltImage(for: result, sourceImage: nil),
                  let index = words.firstIndex(where: { $0.id == id }) else { return }
            let old = words[index]
            let updated = LearnedWord(
                id: old.id, word: old.word, displayNameZh: old.displayNameZh,
                exampleSentence: old.exampleSentence, category: old.category,
                imageURL: url, learnedAt: old.learnedAt
            )
            withAnimation(.easeInOut(duration: 0.4)) { words[index] = updated }
            LocalStore.save(words)
        }
    }

    // MARK: - 今日任务 & 头像

    func saveTasks() { LocalStore.save(tasks) }

    func setAvatar(_ image: UIImage) {
        avatarImage = image
        ProfileStore.saveAvatar(image)
    }

    // MARK: - 后台生成绘本

    /// 启动一本绘本的后台生成：立即在绘本页弹出"生成中"卡片，生成完成后替换为可点击卡片。
    func startStoryGeneration(for result: RecognitionResult, reference: UIImage?, coverURL: URL?) {
        let job = GeneratingStory(
            id: UUID(),
            focusWord: result.word,
            progressDone: 0,
            progressTotal: 4,
            coverURL: coverURL,
            failed: false,
            result: result,
            reference: reference
        )
        withAnimation(.spring(response: 0.45, dampingFraction: 0.68)) {
            generatingStories.insert(job, at: 0)
        }
        runStoryJob(id: job.id)
    }

    func retryStoryGeneration(id: UUID) {
        guard let index = generatingStories.firstIndex(where: { $0.id == id }) else { return }
        generatingStories[index].failed = false
        generatingStories[index].progressDone = 0
        runStoryJob(id: id)
    }

    func dismissStoryJob(id: UUID) {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) {
            generatingStories.removeAll { $0.id == id }
        }
    }

    private func runStoryJob(id: UUID) {
        guard let job = generatingStories.first(where: { $0.id == id }) else { return }
        Task {
            do {
                let story = try await agnes.generateIllustratedStory(
                    for: job.result,
                    reference: job.reference
                ) { done, total in
                    Task { @MainActor in self.updateJobProgress(id: id, done: done, total: total) }
                }
                withAnimation(.spring(response: 0.5, dampingFraction: 0.72)) {
                    generatingStories.removeAll { $0.id == id }
                    stories.insert(story, at: 0)
                }
                LocalStore.save(stories)
            } catch {
                if let index = generatingStories.firstIndex(where: { $0.id == id }) {
                    withAnimation { generatingStories[index].failed = true }
                }
            }
        }
    }

    private func updateJobProgress(id: UUID, done: Int, total: Int) {
        guard let index = generatingStories.firstIndex(where: { $0.id == id }) else { return }
        generatingStories[index].progressDone = done
        generatingStories[index].progressTotal = total
    }
}

/// 后台生成中的绘本占位（内存态，不持久化）。
struct GeneratingStory: Identifiable {
    let id: UUID
    let focusWord: String
    var progressDone: Int
    var progressTotal: Int
    let coverURL: URL?
    var failed: Bool
    let result: RecognitionResult
    let reference: UIImage?
}

enum AppTab: Hashable {
    case home
    case camera
    case stories
    case words
    case history
}
