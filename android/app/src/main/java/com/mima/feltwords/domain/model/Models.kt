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
    val exampleSentenceZh: String = "",
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
    /** 毛毡风插画（生成成功后填入；为空表示仍在生成或生成失败） */
    val imageUrl: String? = null,
    /** 第一次识别时拍下的原图本地路径，毛毡图未就绪时作回退展示 */
    val capturedImagePath: String? = null,
    val recognizedAt: Long = System.currentTimeMillis()
)

/** 绘本单页 */
@Serializable
data class StoryPage(
    val id: String = UUID.randomUUID().toString(),
    val sentence: String,
    val sentenceZh: String = "",
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
