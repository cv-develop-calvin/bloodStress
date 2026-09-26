package org.bp.songbaobao.ui.components

import androidx.compose.ui.graphics.Color
import org.bp.songbaobao.R
import org.bp.songbaobao.domain.CbcItem
import org.bp.songbaobao.ui.theme.*

/** 单项指标偏高/偏低判断 */
enum class CbcJudge(
    val nameRes: Int,
    val color: Color
) {
    HIGH(R.string.judge_high, WarnAmber),
    LOW(R.string.judge_low, DangerRed),
    NORMAL(R.string.judge_normal, SuccessGreen),
    NONE(R.string.judge_dash, Color.Gray);

    companion object {
        fun of(v: Double?, item: CbcItem): CbcJudge {
            if (v == null) return NONE
            if (v < item.low) return LOW
            if (v > item.high) return HIGH
            return NORMAL
        }
    }
}
