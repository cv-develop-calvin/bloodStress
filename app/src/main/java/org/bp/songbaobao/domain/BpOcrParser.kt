package org.bp.songbaobao.domain

/**
 * 把 ML Kit 识别出的血压计文本解析为收缩压 / 舒张压 / 心率(脉搏)。
 *
 * 识别主策略（按用户要求）：**不看标签，只看行的位置**——
 *  - 第 1 行 = 收缩压(SYS)
 *  - 第 2 行 = 舒张压(DIA)
 *  - 第 3 行 = 心率(PUL)
 * 这与绝大多数电子血压计的屏幕排版一致（顶部时间戳会被先剥离，不计入行）。
 *
 * 兜底策略（当行数不足或某行没有数字时）：
 *  1) 标签识别：「收缩压 120」「SBP 118」「脉搏 72」等；
 *  2) 分数式：「120/80」；
 *  3) 其余裸数字按 SYS>DIA>PUL 的合理性排序补齐。
 *
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
    /** 「130/81」分数式。 */
    private val FRAC_RE = Regex("(\\d{2,3})\\s*[/／]\\s*(\\d{2,3})")

    // 各行数字的合理区间
    private val SYS_RANGE = 70..260
    private val DIA_RANGE = 40..160
    private val PUL_RANGE = 30..180

    fun parse(text: String): BpOcrResult {
        var systolic: Int? = null
        var diastolic: Int? = null
        var pulse: Int? = null

        // 0) 先剥离时间戳与日期，避免其数字污染读数（血压计常在顶部显示 HH:MM）
        val cleaned = DATE_RE.replace(TIME_RE.replace(text, " "), " ")

        // 1) 主策略：按行位置落位（第一行 SYS / 第二行 DIA / 第三行 PUL）
        //    先做标签识别，用于在行内排除标签自带的说明数字，并兜底。
        for (line in cleaned.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty()) continue
            if (systolic == null) labeledNumber(l, SYS_ALIASES)?.let { systolic = it }
            if (diastolic == null) labeledNumber(l, DIA_ALIASES)?.let { diastolic = it }
            if (pulse == null) labeledNumber(l, PULSE_ALIASES)?.let { pulse = it }
        }

        // 收集每一行中的数字（按自然顺序），供位置解析使用
        val rowNumbers = cleaned.lineSequence()
            .map { lineNumbers(it) }
            .filter { it.isNotEmpty() }
            .toList()

        // 位置优先：只要某行有数字且该位尚未由标签确定，就采用它
        assignByPosition(
            rowNumbers,
            systolicFixed = systolic,
            diastolicFixed = diastolic,
            pulseFixed = pulse
        )?.let { (s, d, p) ->
            systolic = systolic ?: s
            diastolic = diastolic ?: d
            pulse = pulse ?: p
        }

        // 2) 分数式 X/Y（仅在收缩压/舒张压尚未都识别到时兜底）
        if (systolic == null || diastolic == null) {
            FRAC_RE.find(fixDigits(cleaned))?.let { frac ->
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

        // 3) 最终兜底：对所有裸数字按 SYS>DIA>PUL 合理性补齐
        if (systolic == null || diastolic == null || pulse == null) {
            val nums = collectNumbers(cleaned).toMutableList()
            systolic?.let { nums.remove(it) }
            diastolic?.let { nums.remove(it) }
            pulse?.let { nums.remove(it) }
            assignBareNumbers(nums, systolic, diastolic, pulse)?.let { (s, d, p) ->
                systolic = systolic ?: s
                diastolic = diastolic ?: d
                pulse = pulse ?: p
            }
        }

        return BpOcrResult(systolic, diastolic, pulse, text)
    }

    /**
     * 按行位置落位：第 1 行收缩压、第 2 行舒张压、第 3 行脉搏。
     * 每一行取其首个合理区间内的数字；若该行有多个数字（如两侧各一组读数），取最大值。
     */
    private fun assignByPosition(
        rows: List<List<Int>>,
        systolicFixed: Int?,
        diastolicFixed: Int?,
        pulseFixed: Int?
    ): Triple<Int?, Int?, Int?>? {
        if (rows.isEmpty()) return null
        var s: Int? = null
        var d: Int? = null
        var p: Int? = null

        val row0 = rows.getOrNull(0).orEmpty()
        val row1 = rows.getOrNull(1).orEmpty()
        val row2 = rows.getOrNull(2).orEmpty()

        if (systolicFixed == null) s = bestIn(row0, SYS_RANGE)
        if (diastolicFixed == null) d = bestIn(row1, DIA_RANGE)
        if (pulseFixed == null) p = bestIn(row2, PUL_RANGE)

        // 单行多数字（例如同一行有 SYS/DIA）时的补充：
        // 若第 1 行同时给出两个数字，第二个作为舒张压；
        // 若第 2 行同时给出两个数字，第二个作为脉搏。
        if (diastolicFixed == null && d == null && row0.size >= 2) {
            d = row0.drop(1).lastOrNull { it in DIA_RANGE }
        }
        if (pulseFixed == null && p == null && row1.size >= 2) {
            p = row1.drop(1).lastOrNull { it in PUL_RANGE }
        }

        if (s == null && d == null && p == null) return null
        return Triple(s, d, p)
    }

    /** 取某行中落在区间内的数字；多个时取最大值，单个时直接采用。 */
    private fun bestIn(row: List<Int>, range: IntRange): Int? {
        val hits = row.filter { it in range }
        return if (hits.isEmpty()) null else hits.max()
    }

    /** 提取一行中的所有 2~3 位数字（已做字母→数字还原）。 */
    private fun lineNumbers(line: String): List<Int> {
        val norm = fixDigits(line)
        return NUM.findAll(norm).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
    }

    /**
     * 对裸数字做“合理性排序”：收缩压应最大、舒张压次之、脉搏最小且通常 ≤120。
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
            for (s in cand) {
                for (d in cand) {
                    for (p in cand) {
                        if (s > d && d >= p && s in SYS_RANGE && d in DIA_RANGE && p in PUL_RANGE) {
                            return Triple(s, d, p)
                        }
                    }
                }
            }
            val sorted = numsIn.sortedDescending()
            return Triple(sorted.getOrNull(0), sorted.getOrNull(1), sorted.getOrNull(2))
        }

        // 情况 B：部分已知（常见：只缺脉搏）——剩余数字里挑最小的作脉搏
        if (needPul && !needSys && !needDia) {
            return Triple(null, null, numsIn.minOrNull())
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
            if (v in 30..280) out.add(v)
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
