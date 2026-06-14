package com.mima.feltwords.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Edge TTS 朗读引擎 —— 替换系统 TextToSpeech。
 * 通过 WebSocket 连接微软 Edge TTS 服务获取 MP3，MediaPlayer 播放。
 * 对外接口不变：speak / stop / isSpeaking / shutdown。
 */
class TtsManager(context: Context) {

    companion object {
        private const val TAG = "FeltWordsTTS"
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onFinish: (() -> Unit)? = null
    private var currentId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val cacheDir = File(context.cacheDir, "edge_tts").also { it.mkdirs() }
    private val edgeTts = EdgeTtsClient(cacheDir)
    private var player: MediaPlayer? = null
    private var currentJob: Job? = null

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

    fun speak(text: String, onFinish: (() -> Unit)? = null) {
        stop()
        val id = UUID.randomUUID().toString()
        currentId = id
        this.onFinish = onFinish
        _isSpeaking.value = true

        currentJob = scope.launch {
            try {
                val audioFile = edgeTts.synthesize(text)
                if (currentId != id) {
                    audioFile.delete()
                    return@launch
                }
                playAudio(audioFile, id)
            } catch (e: Exception) {
                Log.e(TAG, "Edge TTS failed, text='${text.take(30)}'", e)
                completeCurrent(id)
            }
        }
    }

    private fun playAudio(file: File, id: String) {
        requestAudioFocus()
        player = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                file.delete()
                completeCurrent(id)
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                file.delete()
                completeCurrent(id)
                true
            }
            prepare()
            start()
        }
    }

    fun stop() {
        val prevId = currentId
        currentId = null
        onFinish = null
        currentJob?.cancel()
        currentJob = null
        player?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) { }
        }
        player = null
        _isSpeaking.value = false
        if (prevId != null) abandonAudioFocus()
    }

    fun shutdown() {
        stop()
        edgeTts.shutdown()
        scope.cancel()
        cleanCache()
    }

    private fun completeCurrent(id: String?) {
        if (id != currentId) return
        currentId = null
        _isSpeaking.value = false
        val callback = onFinish
        onFinish = null
        player?.let {
            try { it.release() } catch (_: Exception) { }
        }
        player = null
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

    private fun cleanCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
