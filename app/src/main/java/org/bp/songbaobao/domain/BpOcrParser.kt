package org.bp.songbaobao.domain

/**
 * 把 ML Kit 识别出的血压计文本解析为收缩压 / 舒张压 / 心率(脉搏)。
 *
 * 常见排版：
 *  - 带标签："收缩压 120"、"舒张压 80"、"脉搏 72"、"SBP 118"、"心率 76"
 *  - 分数式："120/80"、"118/76"
 *  - 裸数字（血压计最常见）：三行或同行排列的 "120 80 72"，无标签
 *
 * “分类”即在此完成：优先按标签 / 分数式落位；都没有时，收集文本里所有
 * 落在合理区间的 2~3 位数字，按 SYS→DIA→PUL 的常见顺序兜底赋值。
 * 另外修正 OCR 常见的数码管误认（如 0→O、1→I/l、5→S、8→B）。
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

    /** OCR 把数码管字母误认成数字时的还原表（仅用于提取数字，不影响中英文标签匹配）。 */
    private val DIGIT_FIX = mapOf(
        'O' to '0', 'o' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', 'i' to '1',
        'Z' to '2', 'S' to '5', 'B' to '8'
    )

    private fun fixDigits(s: String): String =
        s.map { DIGIT_FIX[it] ?: it }.joinToString("")

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
            val frac = Regex("(\\d{2,3})\\s*[/／]\\s*(\\d{2,3})").find(fixDigits(text))
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

        // 3) 兜底：收集全部 2~3 位候选数字，按 SYS→DIA→PUL 顺序赋值
        if (systolic == null || diastolic == null || pulse == null) {
            val nums = collectNumbers(text).toMutableList()
            // 去掉已通过标签/分数式确定的值，避免重复占用
            systolic?.let { nums.remove(it) }
            diastolic?.let { nums.remove(it) }
            var i = 0
            if (systolic == null && i < nums.size) { systolic = nums[i]; i++ }
            if (diastolic == null && i < nums.size) { diastolic = nums[i]; i++ }
            if (pulse == null && i < nums.size) { pulse = nums[i]; i++ }
        }

        return BpOcrResult(systolic, diastolic, pulse, text)
    }

    /** 收集文本里所有落在合理血压/心率区间的 2~3 位数字（已做字母→数字还原）。 */
    private fun collectNumbers(text: String): List<Int> {
        val norm = fixDigits(text)
        val out = mutableListOf<Int>()
        for (m in NUM.findAll(norm)) {
            val v = m.groupValues[1].toIntOrNull() ?: continue
            // 收缩压/舒张压/脉搏的合理范围；排除日期、时间等杂项数字
            if (v in 40..280) out.add(v)
        }
        return out
    }

    /** 在行中找某组别名之后的第一个 2~3 位整数（如「收缩压 120」）。 */
    private fun labeledNumber(line: String, aliases: List<String>): Int? {
        val up = line.uppercase().replace(" ", "").replace("：", ":").replace("，", ",")
        for (alias in aliases.sortedByDescending { it.length }) {
            val a = alias.uppercase().replace(" ", "")
            val idx = up.indexOf(a)
            if (idx >= 0) {
                val after = line.substring(minOf(idx + alias.length, line.length))
                val m = NUM.find(fixDigits(after)) ?: continue
                return m.groupValues[1].toIntOrNull()
            }
        }
        return null
    }
}
