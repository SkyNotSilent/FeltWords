package com.mima.feltwords.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * 英文 TTS 封装 —— 对齐 iOS SpeechService 语义。
 *
 * 语速/音调说明：
 * - iOS AVSpeechSynthesizer: rate=0.36（范围 0-1，默认 0.5），pitch=1.08
 * - Android TextToSpeech: setSpeechRate 默认 1.0，setPitch 默认 1.0
 * - Android rate 0.8 + pitch 1.1 听感上接近 iOS 的慢速童声效果。
 *   这两个常量便于后续微调。
 */
class TtsManager(context: Context) {

    companion object {
        /** 儿童友好语速（Android 默认 1.0；0.8 约为中等偏慢） */
        const val CHILD_FRIENDLY_RATE = 0.7f
        /** 略高音调，听感更活泼 */
        const val CHILD_FRIENDLY_PITCH = 1.1f
    }

    private val _isSpeaking = MutableStateFlow(false)
    /** 当前是否正在朗读 */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /**
     * 当前朗读完成时的回调。引擎不可用/报错也视为完成，避免自动播放卡住；
     * 被 stop() 或新一次 speak() 打断时不触发，手动暂停不会误翻页。
     */
    private var onFinish: (() -> Unit)? = null
    private var currentUtteranceId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** TTS 引擎是否初始化成功 */
    private var ready = false

    private lateinit var tts: TextToSpeech

    init {
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == currentUtteranceId) _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                completeCurrent(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                completeCurrent(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                completeCurrent(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (utteranceId == currentUtteranceId) {
                    currentUtteranceId = null
                    _isSpeaking.value = false
                    onFinish = null
                }
            }
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts.language = Locale.US
                tts.setSpeechRate(CHILD_FRIENDLY_RATE)
                tts.setPitch(CHILD_FRIENDLY_PITCH)
            }
        }
        tts.setOnUtteranceProgressListener(listener)
    }

    /**
     * 朗读文本。开始前先 stop 当前朗读。
     * @param onFinish 完成或引擎不可用时回调（被主动打断不触发）
     */
    fun speak(text: String, onFinish: (() -> Unit)? = null) {
        stop()
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        this.onFinish = onFinish
        if (!ready) {
            completeCurrent(utteranceId)
            return
        }
        _isSpeaking.value = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /** 停止朗读（不触发 onFinish） */
    fun stop() {
        currentUtteranceId = null
        onFinish = null
        tts.stop()
        _isSpeaking.value = false
    }

    /** 释放 TTS 引擎资源 */
    fun shutdown() {
        stop()
        tts.shutdown()
    }

    /**
     * 模拟器未安装语音包或引擎报错时也结束当前页，避免自动播放永远卡在暂停态。
     * 回调统一切回主线程，保证翻页和 Compose 状态更新稳定。
     */
    private fun completeCurrent(utteranceId: String?) {
        if (utteranceId != currentUtteranceId) return
        currentUtteranceId = null
        _isSpeaking.value = false
        val callback = onFinish
        onFinish = null
        if (callback != null) mainHandler.post(callback)
    }
}
