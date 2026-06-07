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
    private val preferences = context.getSharedPreferences("feltwords.profile", Context.MODE_PRIVATE)

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

    /** 每次完整启动前进一张主题图；首次启动返回默认母图。 */
    fun nextMascotThemeIndex(themeCount: Int): Int {
        if (themeCount <= 0) return 0
        val key = "feltwords.mascotDailyThemeIndex"
        val index = if (preferences.contains(key)) {
            preferences.getInt(key, 0).mod(themeCount)
        } else {
            0
        }
        preferences.edit().putInt(key, (index + 1).mod(themeCount)).apply()
        return index
    }
}
