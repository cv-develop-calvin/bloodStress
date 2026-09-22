package org.bp.songbaobao.domain

/**
 * 把 OCR 出来的化验单文本解析为 {指标key -> 数值}。
 * 逻辑移植自 Web 版 lab.py 的 parse_ocr_text。
 *
 * 典型行："WBC 白细胞 12.5 ↑ 3.5-9.5" 或 "HGB 血红蛋白 98 g/L"
 */
object OcrParser {

    private val NUM = Regex("[-+]?\\d+(?:[.,]\\d+)?")
    private val LINE = Regex("^([^0-9]{1,20}?)\\s*[:：]?\\s*($NUM)(.*)$")

    /** 别名（大写、去空格）-> key，长别名优先 */
    private val aliasMap: Map<String, String> by lazy {
        val map = LinkedHashMap<String, String>()
        for (item in CbcItems.items()) {
            for (alias in item.aliases) {
                map[alias.uppercase().replace(" ", "")] = item.key
            }
        }
        map
    }

    private val sortedAliases: List<String> by lazy {
        aliasMap.keys.sortedByDescending { it.length }
    }

    private fun matchKey(text: String): String? {
        val t = text.uppercase().replace(" ", "").replace("：", ":")
        aliasMap[t]?.let { return it }
        // OCR 可能把 % 识别成别的字符，做一次宽松包含匹配
        for (alias in sortedAliases) {
            if (alias.length >= 3 && alias in t) return aliasMap[alias]
        }
        return null
    }

    fun parse(text: String): Map<String, Double> {
        val result = LinkedHashMap<String, Double>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val m = LINE.find(line) ?: continue
            val name = m.groupValues[1]
            val key = matchKey(name) ?: continue
            val rawValue = m.groupValues[2].replace(",", ".")
            val value = rawValue.toDoubleOrNull() ?: continue
            // 参考区间起点（后面紧跟 - 或 ~）不是测量值
            val rest = m.groupValues[3].trim()
            if (rest.startsWith("-") || rest.startsWith("~") || rest.startsWith("—")) continue
            if (!result.containsKey(key)) result[key] = value
        }
        return result
    }
}
