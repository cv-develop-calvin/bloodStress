package org.bp.songbaobao.domain

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

/**
 * 复用同一个中文 [TextRecognizer] 实例。
 *
 * 之前每次识别都 `TextRecognition.getClient(...)` 新建客户端且从不 close，
 * 既浪费内存又拖慢首次识别。这里做进程级复用（ML Kit 客户端本身线程安全）。
 *
 * 使用的是 **内置打包版** `com.google.mlkit:text-recognition-chinese`，
 * 模型随 APK 一起安装，**无需联网下载**，可离线识别。
 */
object TextRecognizerProvider {
    val client: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 把识别过程中的异常转成用户可读的提示，避免像
     * `com.google.mlkit.common.MLKitException: Waiting for the text optional module...`
     * 这类原始英文堆栈直接暴露。
     */
    fun errorMessage(e: Throwable): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("optional module", ignoreCase = true) ||
                raw.contains("downloading", ignoreCase = true) ||
                raw.contains("not downloaded", ignoreCase = true) ->
                "文字识别模型未就绪，请稍后重试，或改用「手动录入」"
            raw.contains("timeout", ignoreCase = true) ->
                "识别超时，请重试或改用「手动录入」"
            else -> "识别失败：${raw.ifBlank { "请重试或改用「手动录入」" }}"
        }
    }
}
