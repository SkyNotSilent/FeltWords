import SwiftUI

@MainActor
final class AppModel: ObservableObject {
    @Published var selectedTab: AppTab = .home
    @Published var words: [LearnedWord] = LocalStore.loadWords()
    @Published var stories: [Storybook] = LocalStore.loadStories()
    /// 正在后台生成的绘本，绘本页用它显示"生成中"占位卡片（最新在前）。
    @Published var generatingStories: [GeneratingStory] = []
    @Published var latestResult: RecognitionResult?
    @Published var capturedImage: UIImage?

    @Published var tasks: [DailyTask] = LocalStore.loadTasks() ?? AppModel.defaultTasks
    @Published var avatarImage: UIImage? = ProfileStore.loadAvatar()

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

    func deleteStory(id: UUID) {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.72)) {
            stories.removeAll { $0.id == id }
        }
        LocalStore.save(stories)
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
}

