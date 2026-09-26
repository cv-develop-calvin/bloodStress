package org.bp.songbaobao.ui.components

import androidx.compose.ui.graphics.Color
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.theme.*

/** 血压分级（参照常用标准）。返回分级名与一句话建议（字符串资源 id，便于国际化）。 */
enum class BpLevel(
    val nameRes: Int,
    val adviceRes: Int,
    val color: Color
) {
    OPTIMAL(R.string.bp_level_optimal, R.string.bp_advice_optimal, GoldBright),
    NORMAL(R.string.bp_level_normal, R.string.bp_advice_normal, GoldBright),
    ELEVATED(R.string.bp_level_elevated, R.string.bp_advice_elevated, Gold),
    STAGE1(R.string.bp_level_stage1, R.string.bp_advice_stage1, Gold),
    STAGE2(R.string.bp_level_stage2, R.string.bp_advice_stage2, WarnAmber),
    CRISIS(R.string.bp_level_crisis, R.string.bp_advice_crisis, DangerRed)
}

/** 血压分级（文件级函数，便于直接 import）。 */
fun classifyBp(sys: Int, dia: Int): BpLevel {
    return when {
        sys >= 180 || dia >= 120 -> BpLevel.CRISIS
        sys >= 160 || dia >= 100 -> BpLevel.STAGE2
        sys >= 140 || dia >= 90 -> BpLevel.STAGE1
        sys >= 130 || dia >= 85 -> BpLevel.ELEVATED
        sys >= 120 -> BpLevel.NORMAL
        else -> BpLevel.OPTIMAL
    }
}
