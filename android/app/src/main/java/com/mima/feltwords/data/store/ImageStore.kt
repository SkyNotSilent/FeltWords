package com.mima.feltwords.data.store

import android.content.Context
import android.graphics.Bitmap
import com.mima.feltwords.data.api.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 生成图片的本地持久化 —— 对齐 iOS GeneratedImageStore。
 * 把远程图片 URL 下载存到 filesDir/generated/，返回本地文件路径字符串。
 */
class ImageStore(context: Context) {

    private val directory: File = File(context.filesDir, "generated").also { it.mkdirs() }

    /**
     * 下载远程图片并保存到本地，返回本地文件的绝对路径。
     * @throws Exception 下载失败时抛出
     */
    suspend fun persist(remoteUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(remoteUrl).build()
        val response = NetworkModule.okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("下载图片失败: ${response.code}")
        }
        val bytes = response.body?.bytes() ?: throw Exception("下载图片为空")
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }

    /** 把本地 Bitmap（如拍照原图）压成 JPEG 存到本地，返回绝对路径。 */
    suspend fun persistBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file.absolutePath
    }

    /** 按文件名在本地目录定位文件，解决重装后旧路径失效的问题 */
    fun resolve(filename: String): String? {
        val file = File(directory, filename)
        return if (file.exists()) file.absolutePath else null
    }
}
