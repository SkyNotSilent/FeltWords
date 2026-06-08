package com.mima.feltwords.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
        private const val TAG = "FeltWordsTTS"
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
    private var initializationFinished = false
    private var pendingSpeech: PendingSpeech? = null

    private lateinit var tts: TextToSpeech
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val audioFocusRequest =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) stop()
                }
                .build()
        } else {
            null
        }

    private data class PendingSpeech(
        val text: String,
        val utteranceId: String,
    )

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
                Log.e(TAG, "TTS playback failed for $utteranceId")
                completeCurrent(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS playback failed for $utteranceId, code=$errorCode")
                completeCurrent(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (utteranceId == currentUtteranceId) {
                    currentUtteranceId = null
                    _isSpeaking.value = false
                    onFinish = null
                    abandonAudioFocus()
                }
            }
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            initializationFinished = true
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = tts.setLanguage(Locale.US)
                ready = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                tts.setSpeechRate(CHILD_FRIENDLY_RATE)
                tts.setPitch(CHILD_FRIENDLY_PITCH)
                tts.setAudioAttributes(audioAttributes)
                Log.i(TAG, "TTS ready=$ready, languageResult=$languageResult")
            } else {
                Log.e(TAG, "TTS initialization failed, status=$status")
            }

            val pending = pendingSpeech
            pendingSpeech = null
            if (pending != null && pending.utteranceId == currentUtteranceId) {
                if (ready) {
                    speakReady(pending.text, pending.utteranceId)
                } else {
                    completeCurrent(pending.utteranceId)
                }
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
            if (!initializationFinished) {
                pendingSpeech = PendingSpeech(text, utteranceId)
            } else {
                completeCurrent(utteranceId)
            }
            return
        }
        speakReady(text, utteranceId)
    }

    private fun speakReady(text: String, utteranceId: String) {
        requestAudioFocus()
        _isSpeaking.value = true
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS rejected speak request for $utteranceId")
            completeCurrent(utteranceId)
        }
    }

    /** 停止朗读（不触发 onFinish） */
    fun stop() {
        currentUtteranceId = null
        onFinish = null
        pendingSpeech = null
        tts.stop()
        _isSpeaking.value = false
        abandonAudioFocus()
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
        abandonAudioFocus()
        if (callback != null) mainHandler.post(callback)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::requestAudioFocus)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
