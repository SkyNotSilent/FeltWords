package com.mima.feltwords.ui.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.data.api.AgnesError
import com.mima.feltwords.data.api.AgnesRepository
import com.mima.feltwords.data.store.LocalStore
import com.mima.feltwords.domain.model.LearnedWord
import com.mima.feltwords.domain.model.RecognitionHistoryItem
import com.mima.feltwords.domain.model.RecognitionResult
import com.mima.feltwords.domain.model.Storybook
import com.mima.feltwords.speech.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 拍照识别主流程 ViewModel —— 对齐 iOS AppModel 中 recognize / save(word) / startStoryGeneration 的语义。
 * 单向数据流：UI 只读 uiState，通过 recognize / saveToWordbook / startStory 驱动状态变更。
 */
class CaptureViewModel : ViewModel() {

    private val repository: AgnesRepository = ServiceLocator.agnesRepository
    private val localStore: LocalStore = ServiceLocator.localStore
    val tts: TtsManager = ServiceLocator.ttsManager

    // ──────────────── 识别流程状态 ────────────────

    /** 识别流程 UI 状态 */
    sealed interface UiState {
        /** 空闲/等待拍照 */
        data object Idle : UiState

        /** 识别中 */
        data object Recognizing : UiState

        /** 识别成功 */
        data class Success(
            val result: RecognitionResult,
            val capturedBitmap: Bitmap,
            val savedToWordbook: Boolean = false,
            /** 毛毡图本地路径或远程 URL；null 表示仍在生成或失败 */
            val feltImageUrl: String? = null,
            /** 是否正在生成毛毡图 */
            val generatingFeltImage: Boolean = false,
            /** 绘本生成中标记 */
            val generatingStory: Boolean = false,
        ) : UiState

        /** 识别失败 */
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 本地单词本缓存，用于 isSavedToWordbook 判断 */
    private val _words = MutableStateFlow<List<LearnedWord>>(emptyList())
    val words: StateFlow<List<LearnedWord>> = _words.asStateFlow()

    init {
        // 初始加载单词本
        viewModelScope.launch {
            _words.value = localStore.loadWords()
        }
    }

    // ──────────────── 识别 ────────────────

    /**
     * 拍照/选图后调用，启动识别流程。
     * 识别成功后自动写入历史（对齐 iOS AppModel.save(history)），
     * 然后异步生成毛毡图（失败静默回退，对齐 iOS 行为）。
     */
    fun recognize(bitmap: Bitmap) {
        _uiState.value = UiState.Recognizing
        viewModelScope.launch {
            try {
                val result = repository.recognize(bitmap)

                // 写入识别历史
                val history = localStore.loadHistory().toMutableList()
                val historyItem = RecognitionHistoryItem(result = result)
                history.add(0, historyItem)
                localStore.saveHistory(history)

                _uiState.value = UiState.Success(
                    result = result,
                    capturedBitmap = bitmap,
                    savedToWordbook = isWordSaved(result.word),
                    generatingFeltImage = true,
                )

                // 异步生成毛毡图 —— 失败时静默回退到原图，不打断体验
                launch {
                    val feltUrl = try {
                        repository.generateFeltImage(result, sourceImage = bitmap)
                    } catch (_: Exception) {
                        null
                    }
                    val current = _uiState.value
                    if (current is UiState.Success) {
                        _uiState.value = current.copy(
                            feltImageUrl = feltUrl,
                            generatingFeltImage = false,
                        )
                    }
                }
            } catch (e: AgnesError) {
                _uiState.value = UiState.Error(e.message ?: "识别失败，请再试一次。")
            } catch (_: Exception) {
                _uiState.value = UiState.Error("毛毛刚才没看清楚，请再试一次吧。")
            }
        }
    }

    // ──────────────── 保存到单词本 ────────────────

    /**
     * 收藏到单词本 —— 去重后存入 LocalStore（对齐 iOS AppModel.save(word)）。
     */
    fun saveToWordbook(result: RecognitionResult, imageUrl: String?) {
        viewModelScope.launch {
            val current = localStore.loadWords().toMutableList()
            // 去重：同一个 word 不重复添加
            if (current.any { it.word == result.word }) return@launch

            val word = LearnedWord(
                word = result.word,
                displayNameZh = result.displayNameZh,
                exampleSentence = result.exampleSentence,
                category = result.category,
                imageUrl = imageUrl,
            )
            current.add(0, word)
            localStore.saveWords(current)
            _words.value = current

            // 更新 UI 状态
            val state = _uiState.value
            if (state is UiState.Success) {
                _uiState.value = state.copy(savedToWordbook = true)
            }
        }
    }

    // ──────────────── 生成绘本 ────────────────

    /**
     * 发起绘本生成（最小版本）—— 调用 generateIllustratedStory 并存入 stories。
     * 对齐 iOS AppModel.startStoryGeneration。
     * 完整的"生成中卡片/重试"留到 P3。
     */
    fun startStory(result: RecognitionResult, referenceBitmap: Bitmap?) {
        val state = _uiState.value
        if (state is UiState.Success) {
            _uiState.value = state.copy(generatingStory = true)
        }
        viewModelScope.launch {
            try {
                val storybook = repository.generateIllustratedStory(
                    result = result,
                    reference = referenceBitmap,
                )
                // 存入本地
                val stories = localStore.loadStories().toMutableList()
                stories.add(0, storybook)
                localStore.saveStories(stories)
            } catch (_: Exception) {
                // P2 最小版本：生成失败时静默处理，P3 会加重试/状态反馈
            } finally {
                val s = _uiState.value
                if (s is UiState.Success) {
                    _uiState.value = s.copy(generatingStory = false)
                }
            }
        }
    }

    // ──────────────── 重置 ────────────────

    /** 返回空闲状态（从结果页返回拍照页时调用） */
    fun resetToIdle() {
        tts.stop()
        _uiState.value = UiState.Idle
    }

    // ──────────────── 工具方法 ────────────────

    private fun isWordSaved(word: String): Boolean =
        _words.value.any { it.word == word }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
    }
}
