package org.bp.songbaobao.domain

/**
 * 把 ML Kit 识别出的血压计文本解析为收缩压 / 舒张压 / 心率(脉搏)。
 *
 * 主策略（按用户要求）：**不看标签，只按数字在照片中的出现顺序**——
 *   第 1 个血压数值 = 收缩压(SYS)
 *   第 2 个血压数值 = 舒张压(DIA)
 *   第 3 个血压数值 = 心率(PUL)
 * 这与绝大多数电子血压计的排版一致（读数在屏幕上自上而下/自左向右排列）。
 *
 * 处理细节：
 *  - 先剥离顶部时间戳(HH:MM)与日期，避免其数字污染读数；
 *  - 去掉空格后，数字可能被识别成「数码管分段」形式（如 "1 3 0"），这里统一按
 *    2~3 位、且落在血压/心率合理区间(30~280)内做贪心切分，兼容各种排版；
 *  - 兜底：识别到 "130/81" 形式的分数时，较大的作为收缩压、较小的作为舒张压；
 *  - 修正 OCR 常见的数码管误认（0→O、1→I/l、5→S、8→B）。
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

    /** OCR 把数码管字母误认成数字时的还原表。 */
    private val DIGIT_FIX = mapOf(
        'O' to '0', 'o' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', 'i' to '1',
        'Z' to '2', 'S' to '5', 'B' to '8'
    )

    private fun fixDigits(s: String): String =
        s.map { DIGIT_FIX[it] ?: it }.joinToString("")

    /** 时间戳 HH:MM / HH：MM —— 从候选中剥离，避免误当读数。 */
    private val TIME_RE = Regex("\\d{1,2}\\s*[:：]\\s*\\d{2}")
    /** 日期 yyyy-MM-dd / yyyy/MM/dd 等（仅匹配含 4 位年份，避免误吞 "130/81" 这类读数）。 */
    private val DATE_RE = Regex("\\d{4}\\s*[-/.]\\s*\\d{1,2}\\s*[-/.]\\s*\\d{1,2}")
    /** 「130/81」分数式。 */
    private val FRAC_RE = Regex("(\\d{2,3})\\s*[/／]\\s*(\\d{2,3})")

    fun parse(text: String): BpOcrResult {
        var systolic: Int? = null
        var diastolic: Int? = null
        var pulse: Int? = null

        // 0) 先剥离时间戳与日期，避免其数字污染读数
        val cleaned = DATE_RE.replace(TIME_RE.replace(text, " "), " ")

        // 1) 按文档顺序收集数值（去掉空格、合并数码管分段），再依次填入 收缩→舒张→心率
        val values = collectOrderedValues(cleaned)
        for (v in values) {
            when {
                systolic == null -> systolic = v
                diastolic == null -> diastolic = v
                pulse == null -> pulse = v
            }
        }

        // 2) 分数式 X/Y 兜底（仅在收缩压/舒张压尚未都识别到时）
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

        return BpOcrResult(systolic, diastolic, pulse, text)
    }

    /**
     * 收集文档中所有「合理血压/心率数值」(30~280)，保持出现顺序。
     * 数字可能被空格分段（"1 3 0"）或连写（"130 81 78"→去空格后 "1308178"），
     * 这里对每段纯数字串做 2~3 位的贪心切分，并只保留落在区间内的结果。
     */
    private fun collectOrderedValues(cleaned: String): List<Int> {
        val out = mutableListOf<Int>()
        for (line in cleaned.lineSequence()) {
            val norm = fixDigits(line).replace("\\s+".toRegex(), "")
            for (m in "\\d+".toRegex().findAll(norm)) {
                out += partitionDigits(m.value)
            }
        }
        return out
    }

    /** 把一段连续数字（长度可能 >3）贪心切成 2~3 位、且落在 30~280 的数值。 */
    private fun partitionDigits(run: String): List<Int> {
        val result = mutableListOf<Int>()
        var i = 0
        while (i < run.length) {
            val three = run.substring(i, minOf(i + 3, run.length)).toIntOrNull()
            val two = run.substring(i, minOf(i + 2, run.length)).toIntOrNull()
            when {
                three != null && three in 30..280 -> { result.add(three); i += 3 }
                two != null && two in 30..280 -> { result.add(two); i += 2 }
                else -> { i += 1 } // 非法片段，跳过一位继续
            }
        }
        return result
    }
}
