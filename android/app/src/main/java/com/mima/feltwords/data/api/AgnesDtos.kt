package com.mima.feltwords.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ──────────────────── Chat 请求 ────────────────────

/** 多模态聊天请求（识别图片） */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: List<ChatContent>
)

@Serializable
data class ChatContent(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrlPayload? = null
)

@Serializable
data class ImageUrlPayload(val url: String)

/** 纯文本聊天请求（故事生成） */
@Serializable
data class TextChatRequest(
    val model: String,
    val messages: List<TextChatMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int
)

@Serializable
data class TextChatMessage(
    val role: String,
    val content: String
)

// ──────────────────── Chat 响应 ────────────────────

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
    val message: ChatResponseMessage
)

@Serializable
data class ChatResponseMessage(
    val content: String? = null
)

// ──────────────────── 图片生成请求 ────────────────────

@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    val size: String,
    val tags: List<String>? = null,
    @SerialName("extra_body") val extraBody: ImageExtraBody? = null
)

@Serializable
data class ImageExtraBody(
    val image: List<String>,
    @SerialName("response_format") val responseFormat: String
)

// ──────────────────── 图片生成响应 ────────────────────

@Serializable
data class ImageResponse(
    val data: List<ImageItem>
)

@Serializable
data class ImageItem(
    val url: String? = null
)

// ──────────────────── 故事文案（内部） ────────────────────

@Serializable
data class GeneratedStory(
    val title: String,
    val sentences: List<String>
)

// ──────────────────── 错误响应 ────────────────────

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorDetail
)

@Serializable
data class ApiErrorDetail(
    val message: String
)
