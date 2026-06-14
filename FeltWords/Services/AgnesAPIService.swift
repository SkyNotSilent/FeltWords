import Foundation
import UIKit

enum AgnesError: LocalizedError {
    case missingAPIKey
    case invalidImage
    case invalidResponse
    case server(String)

    var errorDescription: String? {
        switch self {
        case .missingAPIKey: "请先在 Config/Secrets.xcconfig 中配置 Agnes API Key。"
        case .invalidImage: "这张照片暂时读不出来，请再拍一次。"
        case .invalidResponse: "毛毛没有看清楚，请再试一次。"
        case .server(let message): message
        }
    }
}

final class AgnesAPIService {
    private let baseURL = URL(string: "https://apihub.agnes-ai.com/v1")!
    private let session: URLSession
    private let limiter = RequestRateLimiter(limit: 20, interval: 60)

    init(session: URLSession = .shared) {
        self.session = session
    }

    func recognize(image: UIImage) async throws -> RecognitionResult {
        guard let imageData = image.resized(maxDimension: 1200).jpegData(compressionQuality: 0.72) else {
            throw AgnesError.invalidImage
        }
        let dataURL = "data:image/jpeg;base64,\(imageData.base64EncodedString())"
        let prompt = """
        Identify the single most prominent child-safe object in this photo. Return JSON only:
        {"word":"one lowercase English noun","displayNameZh":"简体中文","confidence":0.0,"category":"food|animal|toy|home|nature|transport|other","childFriendlyDefinition":"short simple English","exampleSentence":"3-7 word child-friendly English sentence","exampleSentenceZh":"例句的简短中文翻译","visualDescription":"short visual description for recreating this exact object as an illustration","alternatives":["up to 3 lowercase nouns"]}.
        If the image contains a person, identify a safe visible object instead. Never identify identity, age, race, or sensitive traits.
        """

        let body = ChatRequest(
            model: "agnes-2.0-flash",
            messages: [.init(role: "user", content: [
                .init(type: "text", text: prompt, imageURL: nil),
                .init(type: "image_url", text: nil, imageURL: .init(url: dataURL))
            ])],
            temperature: 0.1,
            maxTokens: 500
        )
        let response: ChatResponse = try await post(path: "chat/completions", body: body)
        guard let content = response.choices.first?.message.content else { throw AgnesError.invalidResponse }
        return try JSONDecoder().decode(RecognitionResult.self, from: Data(content.cleanedJSON.utf8))
    }

    func generateFeltImage(for result: RecognitionResult, sourceImage: UIImage?) async throws -> URL {
        let prompt = """
        A polished children's picture-book illustration of \(result.visualDescription), clearly recognizable as a \(result.word).
        Handmade wool felt applique style, soft stitched edges, bright sky blue and sunshine yellow accents,
        centered object, warm friendly lighting, simple clean background, no text, no people, child-safe.
        """
        let sourceDataURL: String?
        if let data = sourceImage?.resized(maxDimension: 1024).jpegData(compressionQuality: 0.68) {
            sourceDataURL = "data:image/jpeg;base64,\(data.base64EncodedString())"
        } else {
            sourceDataURL = nil
        }
        let imageToImageBody = ImageRequest(
            model: "agnes-image-2.1-flash",
            prompt: prompt,
            size: "1024x1024",
            tags: sourceDataURL == nil ? nil : ["img2img"],
            extraBody: sourceDataURL.map { .init(image: [$0], responseFormat: "url") }
        )
        let response: ImageResponse
        do {
            response = try await post(path: "images/generations", body: imageToImageBody)
        } catch where sourceDataURL != nil {
            let fallback = ImageRequest(model: "agnes-image-2.1-flash", prompt: prompt, size: "1024x1024", tags: nil, extraBody: nil)
            response = try await post(path: "images/generations", body: fallback)
        }
        guard let rawURL = response.data.first?.url, let url = URL(string: rawURL) else {
            throw AgnesError.invalidResponse
        }
        return (try? await GeneratedImageStore.persist(remoteURL: url)) ?? url
    }

    /// 连环画：先写故事文案，再用毛毡图作参考图为每一页单独生成场景插画，
    /// 保证跨页角色/风格一致，同时画面随句子变化。每完成一页回调一次进度。
    func generateIllustratedStory(
        for result: RecognitionResult,
        reference: UIImage?,
        onProgress: @escaping @Sendable (Int, Int) -> Void
    ) async throws -> Storybook {
        let generated = try await generateStoryText(for: result)
        let sentences = generated.sentences
        let total = sentences.count

        // 参考图先压缩成 base64，供每页 img2img 复用，保持画面连续性。
        let referenceDataURL = reference?.resized(maxDimension: 1024)
            .jpegData(compressionQuality: 0.68)
            .map { "data:image/jpeg;base64,\($0.base64EncodedString())" }

        let completed = Counter()
        // 并行生成各页插画；限流 actor 保证不超过 20/min。
        let urls: [URL?] = try await withThrowingTaskGroup(of: (Int, URL?).self) { group in
            for (index, sentence) in sentences.enumerated() {
                group.addTask {
                    let url = try? await self.generatePageIllustration(
                        word: result.word,
                        visualDescription: result.visualDescription,
                        sentence: sentence,
                        referenceDataURL: referenceDataURL
                    )
                    let done = await completed.increment()
                    onProgress(done, total)
                    return (index, url)
                }
            }
            var slots = [URL?](repeating: nil, count: total)
            for try await (index, url) in group { slots[index] = url }
            return slots
        }

        let zhList = generated.sentencesZh ?? []
        return Storybook(
            id: UUID(),
            title: generated.title,
            focusWord: result.word,
            createdAt: .now,
            pages: sentences.enumerated().map { index, sentence in
                StoryPage(
                    id: UUID(),
                    sentence: sentence,
                    sentenceZh: index < zhList.count ? zhList[index] : "",
                    imageURL: urls[index]
                )
            }
        )
    }

    private func generateStoryText(for result: RecognitionResult) async throws -> GeneratedStory {
        let prompt = """
        Create a four-page English story for a 3-6 year old about "\(result.word)".
        Return JSON only: {"title":"short title","sentences":["3-7 words","3-7 words","3-7 words","3-7 words"],"sentencesZh":["中文翻译","中文翻译","中文翻译","中文翻译"]}.
        Keep it gentle, concrete, positive, and use the word \(result.word) on every page.
        """
        let body = TextChatRequest(
            model: "agnes-2.0-flash",
            messages: [.init(role: "user", content: prompt)],
            temperature: 0.5,
            maxTokens: 350
        )
        let response: ChatResponse = try await post(path: "chat/completions", body: body)
        guard let content = response.choices.first?.message.content else { throw AgnesError.invalidResponse }
        let generated = try JSONDecoder().decode(GeneratedStory.self, from: Data(content.cleanedJSON.utf8))
        guard !generated.sentences.isEmpty else { throw AgnesError.invalidResponse }
        return generated
    }

    /// 单页场景插画：以毛毡图为参考做 img2img，prompt 锁风格 + 场景。失败回退纯 text2img。
    private func generatePageIllustration(
        word: String,
        visualDescription: String,
        sentence: String,
        referenceDataURL: String?
    ) async throws -> URL {
        let prompt = """
        A children's picture-book illustration in handmade wool felt applique style, soft stitched edges,
        bright sky blue and sunshine yellow accents, warm friendly lighting, simple clean background, no text, no people, child-safe.
        Keep the \(word) (\(visualDescription)) looking consistent with the reference image across the story.
        Scene: \(sentence)
        """
        let body = ImageRequest(
            model: "agnes-image-2.1-flash",
            prompt: prompt,
            size: "1024x1024",
            tags: referenceDataURL == nil ? nil : ["img2img"],
            extraBody: referenceDataURL.map { .init(image: [$0], responseFormat: "url") }
        )
        let response: ImageResponse
        do {
            response = try await post(path: "images/generations", body: body)
        } catch where referenceDataURL != nil {
            let fallback = ImageRequest(model: "agnes-image-2.1-flash", prompt: prompt, size: "1024x1024", tags: nil, extraBody: nil)
            response = try await post(path: "images/generations", body: fallback)
        }
        guard let rawURL = response.data.first?.url, let url = URL(string: rawURL) else {
            throw AgnesError.invalidResponse
        }
        return (try? await GeneratedImageStore.persist(remoteURL: url)) ?? url
    }

    private func post<Body: Encodable, Response: Decodable>(path: String, body: Body) async throws -> Response {
        try await limiter.waitForSlot()
        guard let apiKey = Bundle.main.object(forInfoDictionaryKey: "AGNES_API_KEY") as? String,
              !apiKey.isEmpty, !apiKey.contains("$(") else {
            throw AgnesError.missingAPIKey
        }
        var request = URLRequest(url: baseURL.appending(path: path))
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(body)
        request.timeoutInterval = 90

        let (data, urlResponse) = try await session.data(for: request)
        guard let http = urlResponse as? HTTPURLResponse else { throw AgnesError.invalidResponse }
        guard 200..<300 ~= http.statusCode else {
            let message = (try? JSONDecoder().decode(APIErrorResponse.self, from: data).error.message)
                ?? "服务有点忙，请稍后再试。"
            throw AgnesError.server(message)
        }
        return try JSONDecoder().decode(Response.self, from: data)
    }
}

private struct ChatRequest: Encodable {
    let model: String
    let messages: [Message]
    let temperature: Double
    let maxTokens: Int

    enum CodingKeys: String, CodingKey {
        case model, messages, temperature
        case maxTokens = "max_tokens"
    }

    struct Message: Encodable {
        let role: String
        let content: [Content]
    }

    struct Content: Encodable {
        let type: String
        let text: String?
        let imageURL: ImageURL?

        enum CodingKeys: String, CodingKey {
            case type, text
            case imageURL = "image_url"
        }
    }

    struct ImageURL: Encodable { let url: String }
}

private struct TextChatRequest: Encodable {
    let model: String
    let messages: [Message]
    let temperature: Double
    let maxTokens: Int

    enum CodingKeys: String, CodingKey {
        case model, messages, temperature
        case maxTokens = "max_tokens"
    }

    struct Message: Encodable {
        let role: String
        let content: String
    }
}

private struct ChatResponse: Decodable {
    let choices: [Choice]
    struct Choice: Decodable { let message: Message }
    struct Message: Decodable { let content: String }
}

private struct ImageRequest: Encodable {
    let model: String
    let prompt: String
    let size: String
    let tags: [String]?
    let extraBody: ExtraBody?

    enum CodingKeys: String, CodingKey {
        case model, prompt, size, tags
        case extraBody = "extra_body"
    }

    struct ExtraBody: Encodable {
        let image: [String]
        let responseFormat: String

        enum CodingKeys: String, CodingKey {
            case image
            case responseFormat = "response_format"
        }
    }
}

private struct ImageResponse: Decodable {
    let data: [Item]
    struct Item: Decodable { let url: String? }
}

private struct GeneratedStory: Decodable {
    let title: String
    let sentences: [String]
    let sentencesZh: [String]?
}

private struct APIErrorResponse: Decodable {
    let error: APIError
    struct APIError: Decodable { let message: String }
}

/// 并发任务里安全累加已完成数量。
private actor Counter {
    private var value = 0
    func increment() -> Int {
        value += 1
        return value
    }
}

private extension String {
    var cleanedJSON: String {
        replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private extension UIImage {
    func resized(maxDimension: CGFloat) -> UIImage {
        let scale = min(1, maxDimension / max(size.width, size.height))
        guard scale < 1 else { return self }
        let target = CGSize(width: size.width * scale, height: size.height * scale)
        return UIGraphicsImageRenderer(size: target).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
