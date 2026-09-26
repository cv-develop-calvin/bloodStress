package org.bp.songbaobao.domain

import org.bp.songbaobao.R

/** 单条血常规指标定义 */
data class CbcItem(
    val key: String,
    val labelRes: Int,
    val unit: String,
    val low: Double,
    val high: Double,
    val aliases: List<String>
)

/** 常用血常规项目（与 Web 版 lab.py 一致）。label 用字符串资源，便于国际化。 */
object CbcItems {
    private val list = listOf(
        CbcItem("wbc", R.string.cbc_wbc, "10⁹/L", 4.0, 10.0, listOf("WBC", "白细胞", "白细胞计数")),
        CbcItem("rbc", R.string.cbc_rbc, "10¹²/L", 3.8, 5.8, listOf("RBC", "红细胞", "红细胞计数")),
        CbcItem("hgb", R.string.cbc_hgb, "g/L", 115.0, 175.0, listOf("HGB", "HB", "血红蛋白")),
        CbcItem("hct", R.string.cbc_hct, "%", 35.0, 50.0, listOf("HCT", "红细胞压积", "红细胞比容")),
        CbcItem("mcv", R.string.cbc_mcv, "fL", 82.0, 100.0, listOf("MCV", "平均红细胞体积")),
        CbcItem("mch", R.string.cbc_mch, "pg", 27.0, 34.0, listOf("MCH", "平均血红蛋白量")),
        CbcItem("mchc", R.string.cbc_mchc, "g/L", 316.0, 354.0, listOf("MCHC", "平均血红蛋白浓度")),
        CbcItem("plt", R.string.cbc_plt, "10⁹/L", 125.0, 350.0, listOf("PLT", "血小板", "血小板计数")),
        CbcItem("lymPct", R.string.cbc_lym, "%", 20.0, 50.0, listOf("LYM%", "LYMPH%", "淋巴细胞比率", "淋巴细胞百分比")),
        CbcItem("neutPct", R.string.cbc_neut, "%", 40.0, 75.0, listOf("NEUT%", "NEU%", "中性粒细胞比率", "中性粒细胞百分比")),
        CbcItem("monoPct", R.string.cbc_mono, "%", 3.0, 10.0, listOf("MONO%", "单核细胞比率")),
        CbcItem("eosPct", R.string.cbc_eos, "%", 0.4, 8.0, listOf("EO%", "EOS%", "嗜酸性粒细胞比率")),
        CbcItem("crp", R.string.cbc_crp, "mg/L", 0.0, 3.0, listOf("hs-CRP", "CRP", "超敏C反应蛋白", "C反应蛋白"))
    )

    val ORDER: List<String> = list.map { it.key }
    private val byKey: Map<String, CbcItem> = list.associateBy { it.key }

    fun get(key: String): CbcItem = byKey[key]
        ?: error("未知血常规指标: $key")

    fun items(): List<CbcItem> = list

    /** 判定单个指标值是否异常 */
    fun isAbnormal(key: String, v: Double?): Boolean {
        if (v == null) return false
        val item = get(key)
        return v < item.low || v > item.high
    }

    /** 统计异常项数量 */
    fun abnormalCount(values: Map<String, Double?>): Int =
        ORDER.count { isAbnormal(it, values[it]) }
}
