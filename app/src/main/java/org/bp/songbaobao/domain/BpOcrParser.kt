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

    /** 时间戳 HH:MM / HH：MM，以及孤立的 HH:MM 片段——从候选中剥离，避免误当读数。 */
    private val TIME_RE = Regex("\\d{1,2}\\s*[:：]\\s*\\d{2}")
    /** 日期 yyyy-MM-dd / yyyy/MM/dd 等，连同其数字一并剥离。 */
    private val DATE_RE = Regex("\\d{2,4}\\s*[-/.年月]\\s*\\d{1,2}\\s*[-/.月日]?\\s*\\d{0,2}")

    fun parse(text: String): BpOcrResult {
        var systolic: Int? = null
        var diastolic: Int? = null
        var pulse: Int? = null

        // 0) 先剥离时间戳与日期，避免其数字污染读数（血压计常在顶部显示 HH:MM）
        val cleaned = DATE_RE.replace(TIME_RE.replace(text, " "), " ")

        // 1) 标签识别（按行，长别名优先，避免“脉”误命中“脉搏/脉率”）
        for (line in cleaned.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty()) continue
            if (systolic == null) labeledNumber(l, SYS_ALIASES)?.let { systolic = it }
            if (diastolic == null) labeledNumber(l, DIA_ALIASES)?.let { diastolic = it }
            if (pulse == null) labeledNumber(l, PULSE_ALIASES)?.let { pulse = it }
        }

        // 2) 分数式 X/Y（仅在收缩压/舒张压尚未都识别到时兜底）
        if (systolic == null || diastolic == null) {
            val frac = Regex("(\\d{2,3})\\s*[/／]\\s*(\\d{2,3})").find(fixDigits(cleaned))
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

        // 3) 兜底：裸数字排版（血压计最常见，如竖排 130 / 81 / 78）
        if (systolic == null || diastolic == null || pulse == null) {
            val nums = collectNumbers(cleaned).toMutableList()
            // 去掉已通过标签/分数式确定的值，避免重复占用
            systolic?.let { nums.remove(it) }
            diastolic?.let { nums.remove(it) }
            assignBareNumbers(nums, systolic, diastolic, pulse)?.let { (s, d, p) ->
                systolic = systolic ?: s
                diastolic = diastolic ?: d
                pulse = pulse ?: p
            }
        }

        return BpOcrResult(systolic, diastolic, pulse, text)
    }

    /**
     * 对裸数字做“合理性排序”：收缩压应最大、舒张压次之、脉搏最小且通常 ≤120。
     * 若能找到满足该关系的三元组，按此落位；否则退回按出现顺序赋值。
     */
    private fun assignBareNumbers(
        numsIn: MutableList<Int>,
        sysFixed: Int?,
        diaFixed: Int?,
        pulFixed: Int?
    ): Triple<Int?, Int?, Int?>? {
        val needSys = sysFixed == null
        val needDia = diaFixed == null
        val needPul = pulFixed == null
        if (!needSys && !needDia && !needPul) return null

        // 情况 A：三个都缺 —— 尝试找 SYS>DIA>PUL 的合理三元组
        if (needSys && needDia && needPul) {
            val cand = numsIn.distinct().sortedDescending()
            // 收缩压候选应偏大、脉搏偏小
            for (s in cand) {
                for (d in cand) {
                    for (p in cand) {
                        if (s > d && d >= p && s in 70..260 && d in 40..160 && p in 35..150) {
                            return Triple(s, d, p)
                        }
                    }
                }
            }
            // 找不到完美三元组：按“最大=收缩压、次大=舒张压、其后=脉搏”启发式
            val sorted = numsIn.sortedDescending()
            val s = sorted.getOrNull(0)
            val d = sorted.getOrNull(1)
            val p = sorted.getOrNull(2)
            return Triple(s, d, p)
        }

        // 情况 B：部分已知（常见：只缺脉搏）——剩余数字里挑最小的作脉搏
        if (needPul && !needSys && !needDia) {
            val p = numsIn.minOrNull()
            return Triple(null, null, p)
        }
        // 情况 C：其它部分缺失 —— 按出现顺序补齐
        val it = numsIn.iterator()
        val s = if (needSys) (if (it.hasNext()) it.next() else null) else null
        val d = if (needDia) (if (it.hasNext()) it.next() else null) else null
        val p = if (needPul) (if (it.hasNext()) it.next() else null) else null
        return Triple(s, d, p)
    }

    /** 收集文本里所有落在合理血压/心率区间的 2~3 位数字（已做字母→数字还原）。 */
    private fun collectNumbers(text: String): List<Int> {
        val norm = fixDigits(text)
        val out = mutableListOf<Int>()
        for (m in NUM.findAll(norm)) {
            val v = m.groupValues[1].toIntOrNull() ?: continue
            // 收缩压/舒张压/脉搏的合理范围；排除日期、时间等杂项数字
            if (v in 35..280) out.add(v)
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
