package org.bp.songbaobao.ui.components

import androidx.compose.ui.graphics.Color
import org.bp.songbaobao.domain.CbcItem
import org.bp.songbaobao.ui.theme.DangerRed
import org.bp.songbaobao.ui.theme.SuccessGreen
import org.bp.songbaobao.ui.theme.TextDim
import org.bp.songbaobao.ui.theme.WarnAmber

/** 判定结果 */
enum class CbcFlag { LOW, NORMAL, HIGH, NONE }

/**
 * 指标判定结果（带颜色）。
 *
 * 注意：放在 ui 层而非 domain —— domain 需要保持纯数据、不含 Compose 依赖，
 * 否则 Room 实体链路会间接依赖 Compose 类型，导致 KSP/Hilt 报 error.NonExistentClass。
 */
data class CbcJudge(val flag: CbcFlag, val name: String, val color: Color) {
    companion object {
        private val LOW = CbcJudge(CbcFlag.LOW, "偏低", WarnAmber)
        private val HIGH = CbcJudge(CbcFlag.HIGH, "偏高", DangerRed)
        private val NORMAL = CbcJudge(CbcFlag.NORMAL, "正常", SuccessGreen)
        private val NONE = CbcJudge(CbcFlag.NONE, "—", TextDim)

        fun none() = NONE

        fun of(v: Double?, item: CbcItem): CbcJudge = when {
            v == null -> NONE
            v < item.low -> LOW
            v > item.high -> HIGH
            else -> NORMAL
        }
    }
}
