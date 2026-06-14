package com.mima.feltwords.data.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * 图片工具 —— 对齐 iOS 的 UIImage.resized + jpegData + base64。
 */
object ImageUtils {

    /**
     * 等比缩放 Bitmap，使最大边不超过 [maxDimension]。
     * 如果原图已经够小，直接返回原 Bitmap。
     */
    fun resize(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val scale = min(1f, maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height))
        if (scale >= 1f) return bitmap
        val w = (bitmap.width * scale).toInt()
        val h = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    /**
     * 将 Bitmap 压缩为 JPEG 字节数组。
     * @param quality 0..100，对齐 iOS 的 compressionQuality（0..1）× 100。
     */
    fun toJpegBytes(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * 将 Bitmap 转为 data:image/jpeg;base64,... 格式的 URL 字符串。
     */
    fun toDataUrl(bitmap: Bitmap, maxDimension: Int, quality: Int): String {
        val resized = resize(bitmap, maxDimension)
        val bytes = toJpegBytes(resized, quality)
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }
}
