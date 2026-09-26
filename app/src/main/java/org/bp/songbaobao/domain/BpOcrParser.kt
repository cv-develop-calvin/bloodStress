package org.bp.songbaobao.domain

/**
 * 把 ML Kit 识别出的血压计文本解析为收缩压 / 舒张压 / 心率(脉搏)。
 *
 * 常见排版：
 *  - 带标签："收缩压 120"、"舒张压 80"、"脉搏 72"、"SBP 118"、"心率 76"
 *  - 分数式："120/80"、"118/76"
 *  - 混合：  "120/80  72"、"SYS 120 DIA 80 PUL 72"
 *
 * “分类”即在此完成：根据中英文标签或“高/低”分数式，把识别到的数字落到对应字段。
 */
object BpOcrParser {

    data class BpOcrResult(
        val systolic: Int?,
        val diastolic: Int?,
        val pulse: Int?,
        val rawText: String
    ) {
        val recognizedCount: Int
            get() = listOfNotNull(systolic, diastolic, pulse).size
    }

    private val SYS_ALIASES = listOf(
        "收缩压", "收縮壓", "收缩", "高压", "高壓", "SBP", "SYS", "SYSTOLIC"
    )
    private val DIA_ALIASES = listOf(
        "舒张压", "舒張壓", "舒张", "低压", "低壓", "DBP", "DIA", "DIASTOLIC"
    )
    private val PULSE_ALIASES = listOf(
        "脉搏", "脈搏", "脉率", "脈率", "心率", "脉", "脈", "心跳", "PULSE", "HR", "PR", "PUL"
    )

    private val NUM = Regex("(\\d{2,3})")

    fun parse(text: String): BpOcrResult {
        var systolic: Int? = null
        var diastolic: Int? = null
        var pulse: Int? = null

        // 1) 标签识别（按行，长别名优先，避免“脉”误命中“脉搏/脉率”）
        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty()) continue
            if (systolic == null) labeledNumber(l, SYS_ALIASES)?.let { systolic = it }
            if (diastolic == null) labeledNumber(l, DIA_ALIASES)?.let { diastolic = it }
            if (pulse == null) labeledNumber(l, PULSE_ALIASES)?.let { pulse = it }
        }

        // 2) 分数式 X/Y（仅在收缩压/舒张压尚未都识别到时兜底）
        if (systolic == null || diastolic == null) {
            val frac = Regex("(\\d{2,3})\\s*[/／]\\s*(\\d{2,3})").find(text)
            if (frac != null) {
                val a = frac.groupValues[1].toIntOrNull()
                val b = frac.groupValues[2].toIntOrNull()
                if (a != null && b != null) {
                    val hi = maxOf(a, b)
                    val lo = minOf(a, b)
                    if (systolic == null) systolic = hi
                    if (diastolic == null) diastolic = lo
                }
            }
        }

        return BpOcrResult(systolic, diastolic, pulse, text)
    }

    /** 在行中找某组别名之后的第一个 2~3 位整数（如「收缩压 120」）。 */
    private fun labeledNumber(line: String, aliases: List<String>): Int? {
        val up = line.uppercase().replace(" ", "").replace("：", ":").replace("，", ",")
        for (alias in aliases.sortedByDescending { it.length }) {
            val a = alias.uppercase().replace(" ", "")
            val idx = up.indexOf(a)
            if (idx >= 0) {
                val after = line.substring(minOf(idx + alias.length, line.length))
                val m = NUM.find(after) ?: continue
                return m.groupValues[1].toIntOrNull()
            }
        }
        return null
    }
}
