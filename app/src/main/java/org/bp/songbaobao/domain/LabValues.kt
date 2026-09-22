package org.bp.songbaobao.domain

/**
 * 血常规指标取值的纯数据契约。
 *
 * 注意：这个接口必须独立于 CbcItems.kt。
 * 因为 CbcItems.kt 引用了 Compose 的 Color（见 CbcJudge），
 * 而 Room 实体 LabReport 实现了本接口 —— 若接口与 Compose 类型同处一个文件，
 * KSP/Hilt 在处理 @Entity / @Database 时会尝试解析 Compose 编译期类型，
 * 从而报 "error.NonExistentClass"（表现为 AppDatabase、DAO 全部无法解析）。
 */
interface LabValues {
    val wbc: Double?
    val rbc: Double?
    val hgb: Double?
    val hct: Double?
    val mcv: Double?
    val mch: Double?
    val mchc: Double?
    val plt: Double?
    val lymPct: Double?
    val neutPct: Double?
    val monoPct: Double?
    val eosPct: Double?
    val crp: Double?
}
