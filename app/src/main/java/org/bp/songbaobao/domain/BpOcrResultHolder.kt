package org.bp.songbaobao.domain

/** 拍照识别结果在「扫描页 → 表单页」之间的临时传递（与化验单的 OcrResultHolder 同思路）。 */
object BpOcrResultHolder {
    private var holder: BpOcrParser.BpOcrResult? = null

    fun set(result: BpOcrParser.BpOcrResult) { holder = result }
    fun peek(): BpOcrParser.BpOcrResult? = holder
    fun consume(): BpOcrParser.BpOcrResult? {
        val r = holder
        holder = null
        return r
    }
}
