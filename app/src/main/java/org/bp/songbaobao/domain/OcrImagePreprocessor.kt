package org.bp.songbaobao.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.mlkit.vision.common.InputImage

/**
 * 把拍下来的仪器屏幕照片做轻量预处理，提升 ML Kit 对 7 段数码管大数字的识别率：
 *  - 解码为 ARGB_8888，长边缩放到 [MAX_SIDE]（太小读不到、太大无意义）
 *  - 灰度 + 对比度拉伸，让数码管数字与背景更分明
 *
 * 血压计 / 血糖仪多为“黑底亮字”或“亮底黑字”的数码管，直接喂原图时 ML Kit 经常读不到，
 * 对比度拉伸后召回率明显改善。解码失败时回退到 [InputImage.fromFilePath]。
 */
object OcrImagePreprocessor {

    private const val MAX_SIDE = 1600

    fun preprocess(context: Context, uri: Uri): InputImage {
        val bmp = runCatching { decode(context, uri) }.getOrNull()
        val enhanced = bmp?.let { runCatching { enhance(it) }.getOrNull() }
        return if (enhanced != null) {
            InputImage.fromBitmap(enhanced, 0)
        } else {
            InputImage.fromFilePath(context, uri)
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val src = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        val longSide = maxOf(src.width, src.height)
        val scale = if (longSide > MAX_SIDE) MAX_SIDE.toFloat() / longSide else 1f
        if (scale < 1f) {
            val w = (src.width * scale).toInt().coerceAtLeast(1)
            val h = (src.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(src, w, h, true)
            if (scaled != src) src.recycle()
            return scaled
        }
        return src
    }

    /** 灰度 + 对比度拉伸（线性映射到全 0~255），使数码管数字更突出。 */
    private fun enhance(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val gray = IntArray(px.size)
        var min = 255
        var max = 0
        for (i in px.indices) {
            val r = Color.red(px[i])
            val g = Color.green(px[i])
            val b = Color.blue(px[i])
            val y = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            gray[i] = y
            if (y < min) min = y
            if (y > max) max = y
        }
        val range = (max - min).coerceAtLeast(1)
        val outPx = IntArray(px.size)
        for (i in px.indices) {
            val v = ((gray[i] - min) * 255 / range).coerceIn(0, 255)
            outPx[i] = Color.rgb(v, v, v)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPx, 0, w, 0, 0, w, h)
        return out
    }
}
