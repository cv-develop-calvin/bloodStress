package org.bp.songbaobao.ui.components

import androidx.compose.ui.graphics.Color
import org.bp.songbaobao.ui.theme.ChartPulse
import org.bp.songbaobao.ui.theme.DangerRed
import org.bp.songbaobao.ui.theme.SuccessGreen
import org.bp.songbaobao.ui.theme.TextDim
import org.bp.songbaobao.ui.theme.WarnAmber

/**
 * 血压分级，参考《中国高血压防治指南》。
 * 取收缩压与舒张压中较高的等级。
 *
 * 注意：放在 ui 层而非 domain —— domain 保持纯数据、不含 Compose 依赖，
 * 避免 Room 实体链路间接依赖 Compose 类型导致 KSP/Hilt 报 error.NonExistentClass。
 */
enum class BpLevelKey { LOW, NORMAL, ELEVATED, STAGE1, STAGE2, STAGE3 }

data class BpLevel(
    val key: BpLevelKey,
    val name: String,
    val color: Color,
    val advice: String
)

private fun sysLevel(s: Int) = when {
    s < 90 -> BpLevelKey.LOW
    s < 120 -> BpLevelKey.NORMAL
    s < 140 -> BpLevelKey.ELEVATED
    s < 160 -> BpLevelKey.STAGE1
    s < 180 -> BpLevelKey.STAGE2
    else -> BpLevelKey.STAGE3
}

private fun diaLevel(d: Int) = when {
    d < 60 -> BpLevelKey.LOW
    d < 80 -> BpLevelKey.NORMAL
    d < 90 -> BpLevelKey.ELEVATED
    d < 100 -> BpLevelKey.STAGE1
    d < 110 -> BpLevelKey.STAGE2
    else -> BpLevelKey.STAGE3
}

private val LEVEL_INFO = mapOf(
    BpLevelKey.LOW to Triple("血压偏低", TextDim, "如伴头晕乏力请就医，注意补水与缓慢起身。"),
    BpLevelKey.NORMAL to Triple("正常血压", SuccessGreen, "保持规律作息、低盐饮食与适度运动。"),
    BpLevelKey.ELEVATED to Triple("正常高值", ChartPulse, "建议限盐、控制体重，定期监测血压。"),
    BpLevelKey.STAGE1 to Triple("1 级高血压", WarnAmber, "建议就医评估，改善生活方式并持续监测。"),
    BpLevelKey.STAGE2 to Triple("2 级高血压", DangerRed, "请尽快就诊，遵医嘱服药并记录血压。"),
    BpLevelKey.STAGE3 to Triple("3 级高血压", DangerRed, "血压显著升高，请立即就医或联系医生。")
)

fun classifyBp(systolic: Int, diastolic: Int): BpLevel {
    val a = sysLevel(systolic)
    val b = diaLevel(diastolic)
    val key = if (a.ordinal >= b.ordinal) a else b
    val (name, color, advice) = LEVEL_INFO[key]!!
    return BpLevel(key, name, color, advice)
}
