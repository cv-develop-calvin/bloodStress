package org.bp.songbaobao.domain

/**
 * 解析 ML Kit 识别出的血糖仪文本为血糖值（mmol/L）。
 *
 * 常见排版：
 *  - 带标签："血糖 5.6"、"GLU 5.6"、"葡萄糖 5.6"、"BLOOD SUGAR 5.6"
 *  - 带单位："5.6 mmol/L"
 *
 * 仅在标签附近或合理区间内取第一个小数（血糖仪读数通常为 1~2 位整数 + 小数）。
 */
object GlucoseOcrParser {

    data class GlucoseOcrResult(
        val value: Float?,
        val unit: String?,
        val rawText: String
    ) {
        val recognizedCount: Int
            get() = if (value != null) 1 else 0
    }

    private val ALIASES = listOf(
        "血糖值", "血糖", "葡萄糖", "血葡萄糖",
        "GLUCOSE", "BLOODSUGAR", "BLOODSUGAR", "GLU", "BS"
    )

    /** OCR 把数码管字母误认成数字时的还原表（仅用于提取数字，不影响标签匹配）。 */
    private val DIGIT_FIX = mapOf(
        'O' to '0', 'o' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', 'i' to '1',
        'Z' to '2', 'S' to '5', 'B' to '8'
    )

    private fun fixDigits(s: String): String =
        s.map { DIGIT_FIX[it] ?: it }.joinToString("")

    // 允许小数点为 . 或全角 ．
    private val DECIMAL = Regex("(\\d{1,2}[.\\uFF0E]\\d{1,2})")
    // 少数血糖仪只显示整数（如 6、12），或单位已是 mg/dL 时的整数值
    private val INTEGER = Regex("(\\d{1,2})")

    fun parse(text: String): GlucoseOcrResult {
        var value: Float? = null

        // 1) 标签识别（按行，长别名优先，避免 "血" 误命中 "血糖" 之类）
        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty()) continue
            if (value == null) labeledNumber(l)?.let { value = it }
        }

        // 2) 兜底：取第一个落在合理 mmol/L 区间内的十进制数
        if (value == null) {
            for (m in DECIMAL.findAll(fixDigits(text))) {
                val v = toFloat(m.groupValues[1]) ?: continue
                if (v in 2.0f..33.0f) { value = v; break }
            }
        }

        // 3) 仍没识别：尝试整数（如 5、6、12）
        if (value == null) {
            for (m in INTEGER.findAll(fixDigits(text))) {
                val v = m.groupValues[1].toFloatOrNull() ?: continue
                if (v in 2.0f..33.0f) { value = v; break }
            }
        }

        val unit = when {
            text.contains("mg/dl", ignoreCase = true) || text.contains("毫克") -> "mg/dL"
            text.contains("mmol", ignoreCase = true) -> "mmol/L"
            else -> null
        }

        return GlucoseOcrResult(value, unit, text)
    }

    private fun toFloat(s: String): Float? =
        s.replace('\uFF0E', '.').toFloatOrNull()

    /** 在行中找某组别名之后的第一个小数（如「血糖 5.6」）。 */
    private fun labeledNumber(line: String): Float? {
        val up = line.uppercase().replace(" ", "").replace("：", ":").replace("，", ",")
        for (alias in ALIASES.sortedByDescending { it.length }) {
            val a = alias.uppercase().replace(" ", "")
            val idx = up.indexOf(a)
            if (idx >= 0) {
                val after = line.substring(minOf(idx + alias.length, line.length))
                val m = DECIMAL.find(fixDigits(after)) ?: continue
                return toFloat(m.groupValues[1])
            }
        }
        return null
    }
}
