package com.mima.feltwords.ui.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.data.api.AgnesError
import com.mima.feltwords.data.api.AgnesRepository
import com.mima.feltwords.domain.model.RecognitionResult
import com.mima.feltwords.speech.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 拍照识别主流程 ViewModel —— 对齐 iOS AppModel 中 recognize / save(word) / startStoryGeneration 的语义。
 * 单向数据流：UI 只读 uiState，跨页面数据统一交给 AppViewModel 管理。
 */
class CaptureViewModel : ViewModel() {

    private val repository: AgnesRepository = ServiceLocator.agnesRepository
    val tts: TtsManager = ServiceLocator.ttsManager

    // ──────────────── 识别流程状态 ────────────────

    /** 识别流程 UI 状态 */
    sealed interface UiState {
        /** 空闲/等待拍照 */
        data object Idle : UiState

        /** 识别中 */
        data class Recognizing(val capturedBitmap: Bitmap) : UiState

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

    // ──────────────── 识别 ────────────────

    /**
     * 拍照/选图后调用，启动识别流程。
     * 识别成功后自动写入历史（对齐 iOS AppModel.save(history)），
     * 然后异步生成毛毡图（失败静默回退，对齐 iOS 行为）。
     */
    fun recognize(
        bitmap: Bitmap,
        onRecognized: (RecognitionResult, capturedImagePath: String?) -> String,
        onImageGenerated: (historyId: String, imageUrl: String?) -> Unit,
        isSavedToWordbook: (String) -> Boolean,
    ) {
        _uiState.value = UiState.Recognizing(bitmap)
        viewModelScope.launch {
            try {
                val result = repository.recognize(bitmap)
                // 先持久化拍照原图，作为毛毡图未就绪时的回退展示
                val capturedPath = try {
                    ServiceLocator.imageStore.persistBitmap(bitmap)
                } catch (_: Exception) {
                    null
                }
                val historyId = onRecognized(result, capturedPath)

                _uiState.value = UiState.Success(
                    result = result,
                    capturedBitmap = bitmap,
                    savedToWordbook = isSavedToWordbook(result.word),
                    generatingFeltImage = true,
                )

                // 异步生成毛毡图 —— 失败时静默回退到原图，不打断体验
                launch {
                    val feltUrl = try {
                        repository.generateFeltImage(result, sourceImage = bitmap)
                    } catch (_: Exception) {
                        null
                    }
                    // 始终回写历史（即使用户已离开结果页），避免生成结果被丢弃导致永远停在占位
                    onImageGenerated(historyId, feltUrl)
                    val current = _uiState.value
                    if (current is UiState.Success && current.result.id == result.id) {
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

    fun markSavedToWordbook() {
        val state = _uiState.value
        if (state is UiState.Success) {
            _uiState.value = state.copy(savedToWordbook = true)
        }
    }

    fun markStoryGenerating() {
        val state = _uiState.value
        if (state is UiState.Success) {
            _uiState.value = state.copy(generatingStory = true)
        }
    }

    // ──────────────── 重置 ────────────────

    /** 返回空闲状态（从结果页返回拍照页时调用） */
    fun resetToIdle() {
        tts.stop()
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
    }
}
