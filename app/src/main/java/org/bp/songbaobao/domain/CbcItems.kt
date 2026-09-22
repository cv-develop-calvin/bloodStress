package org.bp.songbaobao.domain

/** 单条血常规指标定义 */
data class CbcItem(
    val key: String,
    val label: String,
    val unit: String,
    val low: Double,
    val high: Double,
    val aliases: List<String>
)

/** 常用血常规项目（与 Web 版 lab.py 一致） */
object CbcItems {
    private val list = listOf(
        CbcItem("wbc", "白细胞", "10⁹/L", 4.0, 10.0, listOf("WBC", "白细胞", "白细胞计数")),
        CbcItem("rbc", "红细胞", "10¹²/L", 3.8, 5.8, listOf("RBC", "红细胞", "红细胞计数")),
        CbcItem("hgb", "血红蛋白", "g/L", 115.0, 175.0, listOf("HGB", "HB", "血红蛋白")),
        CbcItem("hct", "红细胞压积", "%", 35.0, 50.0, listOf("HCT", "红细胞压积", "红细胞比容")),
        CbcItem("mcv", "平均红细胞体积", "fL", 82.0, 100.0, listOf("MCV", "平均红细胞体积")),
        CbcItem("mch", "平均血红蛋白量", "pg", 27.0, 34.0, listOf("MCH", "平均血红蛋白量")),
        CbcItem("mchc", "平均血红蛋白浓度", "g/L", 316.0, 354.0, listOf("MCHC", "平均血红蛋白浓度")),
        CbcItem("plt", "血小板", "10⁹/L", 125.0, 350.0, listOf("PLT", "血小板", "血小板计数")),
        CbcItem("lymPct", "淋巴细胞百分比", "%", 20.0, 50.0, listOf("LYM%", "LYMPH%", "淋巴细胞比率", "淋巴细胞百分比")),
        CbcItem("neutPct", "中性粒细胞百分比", "%", 40.0, 75.0, listOf("NEUT%", "NEU%", "中性粒细胞比率", "中性粒细胞百分比")),
        CbcItem("monoPct", "单核细胞百分比", "%", 3.0, 10.0, listOf("MONO%", "单核细胞比率")),
        CbcItem("eosPct", "嗜酸性粒细胞百分比", "%", 0.4, 8.0, listOf("EO%", "EOS%", "嗜酸性粒细胞比率")),
        CbcItem("crp", "超敏C反应蛋白", "mg/L", 0.0, 3.0, listOf("hs-CRP", "CRP", "超敏C反应蛋白", "C反应蛋白"))
    )

    val ORDER: List<String> = list.map { it.key }
    private val byKey: Map<String, CbcItem> = list.associateBy { it.key }

    fun get(key: String): CbcItem = byKey[key]
        ?: error("未知血常规指标: $key")

    fun items(): List<CbcItem> = list

    /** 按 key 从报告里取值（类型安全：LabReport 实现 LabValues） */
    fun valueOf(report: LabValues, key: String): Double? = when (key) {
        "wbc" -> report.wbc
        "rbc" -> report.rbc
        "hgb" -> report.hgb
        "hct" -> report.hct
        "mcv" -> report.mcv
        "mch" -> report.mch
        "mchc" -> report.mchc
        "plt" -> report.plt
        "lymPct" -> report.lymPct
        "neutPct" -> report.neutPct
        "monoPct" -> report.monoPct
        "eosPct" -> report.eosPct
        "crp" -> report.crp
        else -> null
    }

    /** 统计异常项数量 */
    fun abnormalCount(report: LabValues): Int = ORDER.count { key ->
        val v = valueOf(report, key) ?: return@count false
        val item = get(key)
        v < item.low || v > item.high
    }
}
