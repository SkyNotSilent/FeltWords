package com.mima.feltwords.data.api

import android.graphics.Bitmap
import com.mima.feltwords.BuildConfig
import com.mima.feltwords.data.store.ImageStore
import com.mima.feltwords.data.util.ImageUtils
import com.mima.feltwords.domain.model.RecognitionResult
import com.mima.feltwords.domain.model.Storybook
import com.mima.feltwords.domain.model.StoryPage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Agnes 业务封装 —— 对齐 iOS AgnesAPIService 的三个公开方法。
 * 所有 prompt、模型名、温度、token 数均与 iOS 完全一致。
 */
class AgnesRepository(
    private val imageStore: ImageStore,
    private val api: AgnesApi = NetworkModule.agnesApi,
    private val limiter: RateLimiter = RateLimiter(limit = 20, intervalMs = 60_000L),
    private val json: Json = NetworkModule.json
) {

    // ──────────────────── 1. 拍照识别 ────────────────────

    /**
     * 识别图片中最突出的儿童安全物体，返回 RecognitionResult。
     * - 图片 resize 到 maxDimension=1200，JPEG quality 72%
     * - model=agnes-2.0-flash, temperature=0.1, max_tokens=500
     */
    suspend fun recognize(image: Bitmap): RecognitionResult {
        validateApiKey()

        val dataUrl = ImageUtils.toDataUrl(image, maxDimension = 1200, quality = 72)

        val prompt = """
Identify the single most prominent child-safe object in this photo. Return JSON only:
{"word":"one lowercase English noun","displayNameZh":"简体中文","confidence":0.0,"category":"food|animal|toy|home|nature|transport|other","childFriendlyDefinition":"short simple English","exampleSentence":"3-7 word child-friendly English sentence","exampleSentenceZh":"例句的简体中文翻译","visualDescription":"short visual description for recreating this exact object as an illustration","alternatives":["up to 3 lowercase nouns"]}.
If the image contains a person, identify a safe visible object instead. Never identify identity, age, race, or sensitive traits.
        """.trimIndent()

        val body = ChatRequest(
            model = "agnes-2.0-flash",
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = listOf(
                        ChatContent(type = "text", text = prompt),
                        ChatContent(
                            type = "image_url",
                            imageUrl = ImageUrlPayload(url = dataUrl)
                        )
                    )
                )
            ),
            temperature = 0.1,
            maxTokens = 500
        )

        limiter.waitForSlot()
        val response = try {
            api.chatCompletions(body)
        } catch (e: Exception) {
            throw wrapHttpError(e)
        }

        val content = response.choices.firstOrNull()?.message?.content
            ?: throw AgnesError.InvalidResponse

        return try {
            json.decodeFromString<RecognitionResult>(content.cleanedJson())
        } catch (_: Exception) {
            throw AgnesError.InvalidResponse
        }
    }

    // ──────────────────── 2. 毛毡图生成 ────────────────────

    /**
     * 为识别结果生成毛毡风格插图，返回本地文件路径（优先）或远程 URL。
     * - img2img（extra_body.image + tags=["img2img"]），失败回退纯文生图
     * - model=agnes-image-2.1-flash, size=1024x1024
     */
    suspend fun generateFeltImage(
        result: RecognitionResult,
        sourceImage: Bitmap? = null
    ): String {
        validateApiKey()

        val prompt = buildFeltImagePrompt(result)

        val sourceDataUrl: String? = sourceImage?.let {
            ImageUtils.toDataUrl(it, maxDimension = 1024, quality = 68)
        }

        val body = ImageGenerationRequest(
            model = "agnes-image-2.1-flash",
            prompt = prompt,
            size = "1024x1024",
            tags = if (sourceDataUrl != null) listOf("img2img") else null,
            extraBody = sourceDataUrl?.let {
                ImageExtraBody(image = listOf(it), responseFormat = "url")
            }
        )

        val response = try {
            limiter.waitForSlot()
            api.imageGenerations(body)
        } catch (e: Exception) {
            // img2img 失败时回退纯文生图
            if (sourceDataUrl != null) {
                val fallback = ImageGenerationRequest(
                    model = "agnes-image-2.1-flash",
                    prompt = prompt,
                    size = "1024x1024"
                )
                limiter.waitForSlot()
                try {
                    api.imageGenerations(fallback)
                } catch (e2: Exception) {
                    throw wrapHttpError(e2)
                }
            } else {
                throw wrapHttpError(e)
            }
        }

        val remoteUrl = response.data.firstOrNull()?.url
            ?: throw AgnesError.InvalidResponse

        // 尝试下载到本地，失败则返回远程 URL
        return try {
            imageStore.persist(remoteUrl)
        } catch (_: Exception) {
            remoteUrl
        }
    }

    // ──────────────────── 3. 连环画绘本生成 ────────────────────

    /**
     * 生成四页英文绘本：先生成故事文案，再并行为每页生成插画。
     * 每完成一页回调 onProgress(done, total)。
     * 限流器保证整体不超 20/min。
     */
    suspend fun generateIllustratedStory(
        result: RecognitionResult,
        reference: Bitmap? = null,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Storybook {
        val generated = generateStoryText(result)
        val sentences = generated.sentences
        val total = sentences.size

        // 参考图先压缩成 base64，供每页 img2img 复用
        val referenceDataUrl: String? = reference?.let {
            ImageUtils.toDataUrl(it, maxDimension = 1024, quality = 68)
        }

        // 线程安全的已完成计数器
        val progressMutex = Mutex()
        var completed = 0

        // 并行生成各页插画，保持页顺序
        val urls: List<String?> = coroutineScope {
            sentences.mapIndexed { _, sentence ->
                async {
                    val url = try {
                        generatePageIllustration(
                            word = result.word,
                            visualDescription = result.visualDescription,
                            sentence = sentence,
                            referenceDataUrl = referenceDataUrl
                        )
                    } catch (_: Exception) {
                        null
                    }
                    // 进度回调
                    val done = progressMutex.withLock {
                        completed++
                        completed
                    }
                    onProgress(done, total)
                    url
                }
            }.awaitAll()
        }

        val zhList = generated.sentencesZh
        return Storybook(
            id = UUID.randomUUID().toString(),
            title = generated.title,
            focusWord = result.word,
            createdAt = System.currentTimeMillis(),
            pages = sentences.mapIndexed { i, sentence ->
                StoryPage(
                    id = UUID.randomUUID().toString(),
                    sentence = sentence,
                    sentenceZh = zhList.getOrElse(i) { "" },
                    imageUrl = urls[i]
                )
            }
        )
    }

    // ──────────────────── 内部方法 ────────────────────

    /** 生成故事文案 —— prompt 逐字对齐 iOS */
    private suspend fun generateStoryText(result: RecognitionResult): GeneratedStory {
        validateApiKey()

        val prompt = """
Create a four-page English story for a 3-6 year old about "${result.word}".
Return JSON only: {"title":"short title","sentences":["3-7 words","3-7 words","3-7 words","3-7 words"],"sentencesZh":["中文翻译","中文翻译","中文翻译","中文翻译"]}.
Keep it gentle, concrete, positive, and use the word ${result.word} on every page. sentencesZh should be simple Chinese translations of each sentence.
        """.trimIndent()

        val body = TextChatRequest(
            model = "agnes-2.0-flash",
            messages = listOf(TextChatMessage(role = "user", content = prompt)),
            temperature = 0.5,
            maxTokens = 350
        )

        limiter.waitForSlot()
        val response = try {
            api.textChatCompletions(body)
        } catch (e: Exception) {
            throw wrapHttpError(e)
        }

        val content = response.choices.firstOrNull()?.message?.content
            ?: throw AgnesError.InvalidResponse

        val story = try {
            json.decodeFromString<GeneratedStory>(content.cleanedJson())
        } catch (_: Exception) {
            throw AgnesError.InvalidResponse
        }

        if (story.sentences.isEmpty()) throw AgnesError.InvalidResponse
        return story
    }

    /** 单页场景插画 —— prompt 逐字对齐 iOS，img2img 失败回退纯文生图 */
    private suspend fun generatePageIllustration(
        word: String,
        visualDescription: String,
        sentence: String,
        referenceDataUrl: String?
    ): String {
        validateApiKey()

        val prompt = """
A children's picture-book illustration in handmade wool felt applique style, soft stitched edges,
bright sky blue and sunshine yellow accents, warm friendly lighting, simple clean background, no text, no people, child-safe.
Keep the $word ($visualDescription) looking consistent with the reference image across the story.
Scene: $sentence
        """.trimIndent()

        val body = ImageGenerationRequest(
            model = "agnes-image-2.1-flash",
            prompt = prompt,
            size = "1024x1024",
            tags = if (referenceDataUrl != null) listOf("img2img") else null,
            extraBody = referenceDataUrl?.let {
                ImageExtraBody(image = listOf(it), responseFormat = "url")
            }
        )

        val response = try {
            limiter.waitForSlot()
            api.imageGenerations(body)
        } catch (e: Exception) {
            if (referenceDataUrl != null) {
                // 回退纯文生图
                val fallback = ImageGenerationRequest(
                    model = "agnes-image-2.1-flash",
                    prompt = prompt,
                    size = "1024x1024"
                )
                limiter.waitForSlot()
                try {
                    api.imageGenerations(fallback)
                } catch (e2: Exception) {
                    throw wrapHttpError(e2)
                }
            } else {
                throw wrapHttpError(e)
            }
        }

        val remoteUrl = response.data.firstOrNull()?.url
            ?: throw AgnesError.InvalidResponse

        // 尝试下载到本地
        return try {
            imageStore.persist(remoteUrl)
        } catch (_: Exception) {
            remoteUrl
        }
    }

    // ──────────────────── 工具函数 ────────────────────

    private fun buildFeltImagePrompt(result: RecognitionResult): String = """
A polished children's picture-book illustration of ${result.visualDescription}, clearly recognizable as a ${result.word}.
Handmade wool felt applique style, soft stitched edges, bright sky blue and sunshine yellow accents,
centered object, warm friendly lighting, simple clean background, no text, no people, child-safe.
    """.trimIndent()

    /** 校验 API Key 是否已配置 */
    private fun validateApiKey() {
        val key = BuildConfig.AGNES_API_KEY
        if (key.isBlank() || key.contains("\$(")) {
            throw AgnesError.MissingApiKey
        }
    }

    /** 将 HTTP 异常包装为 AgnesError */
    private fun wrapHttpError(e: Exception): AgnesError {
        if (e is AgnesError) return e
        return AgnesError.Server(e.message ?: "服务有点忙，请稍后再试。")
    }
}

/** 去掉 AI 返回中可能包含的 ```json``` 围栏 */
private fun String.cleanedJson(): String =
    replace("```json", "")
        .replace("```", "")
        .trim()
