package com.mima.feltwords.speech

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Edge TTS WebSocket 客户端 —— 通过微软 Edge 语音合成服务获取 MP3 音频。
 * 协议对齐 edge-tts 7.x（含 Sec-MS-GEC DRM token）。
 */
class EdgeTtsClient(private val cacheDir: File) {

    companion object {
        private const val TAG = "EdgeTTS"
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        private const val CHROMIUM_MAJOR = "143"
        private const val BASE_URL =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
        private const val VOICE = "en-US-AnaNeural"
        private const val RATE = "-20%"
        private const val PITCH = "+5%"
        private const val WIN_EPOCH = 11644473600L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun generateSecMsGec(): String {
        val nowSec = System.currentTimeMillis() / 1000L
        var ticks = (nowSec + WIN_EPOCH)
        ticks -= ticks % 300
        ticks *= 10_000_000L // 100-nanosecond intervals
        val toHash = "${ticks}$TRUSTED_CLIENT_TOKEN"
        val digest = MessageDigest.getInstance("SHA-256").digest(toHash.toByteArray(Charsets.US_ASCII))
        return digest.joinToString("") { "%02X".format(it) }
    }

    private fun generateMuid(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun connectId(): String = UUID.randomUUID().toString().replace("-", "")

    suspend fun synthesize(text: String): File = withContext(Dispatchers.IO) {
        val requestId = connectId()
        val outputFile = File(cacheDir, "tts_$requestId.mp3")

        val secGec = generateSecMsGec()
        val url = "$BASE_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&ConnectionId=$requestId" +
            "&Sec-MS-GEC=$secGec" +
            "&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION"

        val request = Request.Builder()
            .url(url)
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                    " (KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR.0.0.0 Safari/537.36" +
                    " Edg/$CHROMIUM_MAJOR.0.0.0"
            )
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", "muid=${generateMuid()};")
            .build()

        suspendCancellableCoroutine { cont ->
            val fos = FileOutputStream(outputFile)

            val ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val timestamp = java.text.SimpleDateFormat(
                        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
                        java.util.Locale.US
                    ).format(java.util.Date())

                    val configMessage =
                        "X-Timestamp:${timestamp}\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n" +
                            """{"context":{"synthesis":{"audio":{"metadataoptions":{""" +
                            """"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},""" +
                            """"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}""" +
                            "\r\n"
                    webSocket.send(configMessage)

                    val ssml = buildSsml(text)
                    val ssmlMessage =
                        "X-RequestId:$requestId\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "X-Timestamp:${timestamp}Z\r\n" +
                            "Path:ssml\r\n\r\n" +
                            ssml
                    webSocket.send(ssmlMessage)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val data = bytes.toByteArray()
                    if (data.size > 2) {
                        val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        val audioStart = headerLen + 2
                        if (data.size > audioStart) {
                            fos.write(data, audioStart, data.size - audioStart)
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end")) {
                        fos.close()
                        webSocket.close(1000, null)
                        if (cont.isActive) cont.resume(outputFile)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${response?.code} ${response?.message}", t)
                    runCatching { fos.close() }
                    outputFile.delete()
                    if (cont.isActive) cont.resumeWithException(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (cont.isActive) {
                        runCatching { fos.close() }
                        if (outputFile.length() > 0) {
                            cont.resume(outputFile)
                        } else {
                            outputFile.delete()
                            cont.resumeWithException(RuntimeException("Edge TTS empty audio, code=$code"))
                        }
                    }
                }
            })

            cont.invokeOnCancellation {
                ws.cancel()
                runCatching { fos.close() }
                outputFile.delete()
            }
        }
    }

    private fun buildSsml(text: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
        return """<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="en-US">""" +
            """<voice name="$VOICE">""" +
            """<prosody rate="$RATE" pitch="$PITCH">$escaped</prosody>""" +
            """</voice></speak>"""
    }

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
    }
}
