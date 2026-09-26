package org.bp.songbaobao.domain

/** 拍照识别结果在「扫描页 → 表单页」之间的临时传递（与血压的 OcrResultHolder 同思路）。 */
object GlucoseOcrResultHolder {
    private var holder: GlucoseOcrParser.GlucoseOcrResult? = null

    fun set(result: GlucoseOcrParser.GlucoseOcrResult) { holder = result }
    fun peek(): GlucoseOcrParser.GlucoseOcrResult? = holder
    fun consume(): GlucoseOcrParser.GlucoseOcrResult? {
        val r = holder
        holder = null
        return r
    }
}
