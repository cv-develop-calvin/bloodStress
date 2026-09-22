package org.bp.songbaobao.domain

/**
 * 暂存最近一次 OCR 识别结果。
 *
 * 为什么不走导航参数：
 * 1) OCR 原文可能很长，放进 URL 有长度限制；
 * 2) 拍照页与表单页是不同的 NavBackStackEntry，ViewModel 实例不共享。
 * 这里用进程内单例传递，读取后即清空，避免重复预填。
 */
object OcrResultHolder {

    @Volatile private var text: String = ""
    @Volatile private var values: Map<String, Double> = emptyMap()

    fun set(resultText: String, parsed: Map<String, Double>) {
        text = resultText
        values = parsed
    }

    fun consume(): Pair<String, Map<String, Double>> {
        val pair = text to values
        text = ""
        values = emptyMap()
        return pair
    }

    fun peek(): Pair<String, Map<String, Double>> = text to values

    fun clear() {
        text = ""
        values = emptyMap()
    }
}
