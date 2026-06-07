package com.mima.feltwords.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** 识别结果 —— 对齐 iOS RecognitionResult */
@Serializable
data class RecognitionResult(
    val word: String,
    val displayNameZh: String,
    val confidence: Double,
    val category: String,
    val childFriendlyDefinition: String,
    val exampleSentence: String,
    val visualDescription: String,
    val alternatives: List<String> = emptyList()
) {
    /** 复合 id，与 iOS 保持一致 */
    val id: String get() = "$word-$displayNameZh"
}

/** 已学单词 */
@Serializable
data class LearnedWord(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val displayNameZh: String,
    val exampleSentence: String,
    val category: String,
    val imageUrl: String? = null,
    val learnedAt: Long = System.currentTimeMillis()
)

/** 识别历史条目 */
@Serializable
data class RecognitionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val result: RecognitionResult,
    val imageUrl: String? = null,
    val recognizedAt: Long = System.currentTimeMillis()
)

/** 绘本单页 */
@Serializable
data class StoryPage(
    val id: String = UUID.randomUUID().toString(),
    val sentence: String,
    val imageUrl: String? = null
)

/** 绘本 */
@Serializable
data class Storybook(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val focusWord: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pages: List<StoryPage> = emptyList()
)

/** 今日任务 —— icon 先存字符串标识，UI 阶段再映射成 Material 图标 */
@Serializable
data class DailyTask(
    val id: String = UUID.randomUUID().toString(),
    val icon: String,
    val prefix: String,
    val count: Int,
    val suffix: String
)
