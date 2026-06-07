package com.mima.feltwords.data.store

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 头像本地持久化 —— 对齐 iOS ProfileStore。
 * 固定文件名存到 filesDir/profile/avatar.jpg。
 */
class ProfileStore(context: Context) {

    private val directory: File = File(context.filesDir, "profile").also { it.mkdirs() }
    private val avatarFile: File = File(directory, "avatar.jpg")

    /** 读取头像，不存在返回 null */
    suspend fun loadAvatar(): Bitmap? = withContext(Dispatchers.IO) {
        if (!avatarFile.exists()) return@withContext null
        BitmapFactory.decodeFile(avatarFile.absolutePath)
    }

    /** 保存头像（JPEG quality 85%） */
    suspend fun saveAvatar(image: Bitmap) = withContext(Dispatchers.IO) {
        FileOutputStream(avatarFile).use { out ->
            image.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
    }

    /** 删除头像 */
    suspend fun deleteAvatar() = withContext(Dispatchers.IO) {
        if (avatarFile.exists()) avatarFile.delete()
    }
}
