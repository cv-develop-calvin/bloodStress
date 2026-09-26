package org.bp.songbaobao.ui.components

import androidx.compose.ui.graphics.Color
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.theme.*

/** 血糖测量时段（空腹 / 餐后 / 随机 / 睡前）。key 存入 GlucoseRecord.context。 */
enum class GlucoseContext(
    val key: String,
    val labelRes: Int
) {
    FASTING("fasting", R.string.glucose_ctx_fasting),
    POSTPRANDIAL("postprandial", R.string.glucose_ctx_postprandial),
    RANDOM("random", R.string.glucose_ctx_random),
    BEDTIME("bedtime", R.string.glucose_ctx_bedtime);

    companion object {
        fun fromKey(key: String?): GlucoseContext =
            values().firstOrNull { it.key == key } ?: RANDOM

        fun list(): List<GlucoseContext> = values().toList()
    }
}

/** 血糖分级（mmol/L，参照常用参考区间）。返回分级名与一句话建议（字符串资源 id，便于国际化）。 */
enum class GlucoseLevel(
    val nameRes: Int,
    val adviceRes: Int,
    val color: Color
) {
    LOW(R.string.glucose_level_low, R.string.glucose_advice_low, WarnAmber),
    NORMAL(R.string.glucose_level_normal, R.string.glucose_advice_normal, GoldBright),
    HIGH(R.string.glucose_level_high, R.string.glucose_advice_high, Gold),
    VERY_HIGH(R.string.glucose_level_very_high, R.string.glucose_advice_very_high, DangerRed)
}

/** 按测量时段与数值分级。 */
fun classifyGlucose(value: Float, context: GlucoseContext): GlucoseLevel {
    // 各时段正常上限与偏高（疑似异常）上限
    val (normalMax, veryHighMax) = when (context) {
        GlucoseContext.FASTING -> 6.1f to 7.0f        // 空腹 ≥7.0 提示糖尿病可能
        GlucoseContext.POSTPRANDIAL -> 7.8f to 11.1f   // 餐后2h ≥11.1 提示糖尿病可能
        else -> 11.1f to Float.MAX_VALUE               // 随机 / 睡前
    }
    return when {
        value < 3.9f -> GlucoseLevel.LOW
        value <= normalMax -> GlucoseLevel.NORMAL
        value <= veryHighMax -> GlucoseLevel.HIGH
        else -> GlucoseLevel.VERY_HIGH
    }
}
