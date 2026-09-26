package org.bp.songbaobao.ui.components

import android.graphics.Color as GColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.bp.songbaobao.R

/**
 * 血压趋势图（MPAndroidChart 通过 AndroidView 包装）。
 * 心率量级与血压不同，单独放在右侧轴，避免被压成平线。
 */
@Composable
fun BpLineChart(
    labels: List<String>,
    systolic: List<Float?>,
    diastolic: List<Float?>,
    pulse: List<Float?>,
    showSys: Boolean = true,
    showDia: Boolean = true,
    showPulse: Boolean = true,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(false)
                setDrawGridBackground(false)
                setBackgroundColor(GColor.TRANSPARENT)
                legend.isEnabled = false
                setNoDataText(ctx.getString(R.string.chart_no_data))

                // X 轴
                xAxis.apply {
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = GColor.parseColor("#A8B0D8")
                    textSize = 9f
                    granularity = 1f
                    labelRotationAngle = 0f
                    isGranularityEnabled = true
                }
                // 左轴（血压）
                axisLeft.apply {
                    textColor = GColor.parseColor("#FFDD7A")
                    textSize = 9f
                    setDrawGridLines(true)
                    gridColor = GColor.parseColor("#1AE8C25A")
                    axisMinimum = 40f
                    axisMaximum = 180f
                    granularity = 20f
                    isGranularityEnabled = true
                    removeAllLimitLines()
                    // 参考线：收缩压 140 / 舒张压 90
                    addLimitLine(LimitLine(140f, ctx.getString(R.string.chart_ref_sys, 140)).apply {
                        lineColor = GColor.parseColor("#8CFFDD7A")
                        lineWidth = 1f
                        enableDashedLine(6f, 4f, 0f)
                        textColor = GColor.parseColor("#8CFFDD7A")
                        textSize = 8f
                        labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    })
                    addLimitLine(LimitLine(90f, ctx.getString(R.string.chart_ref_dia, 90)).apply {
                        lineColor = GColor.parseColor("#8C7FE3C0")
                        lineWidth = 1f
                        enableDashedLine(6f, 4f, 0f)
                        textColor = GColor.parseColor("#8C7FE3C0")
                        textSize = 8f
                        labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                    })
                }
                // 右轴（心率）
                axisRight.apply {
                    textColor = GColor.parseColor("#A99CFF")
                    textSize = 9f
                    setDrawGridLines(false)
                    axisMinimum = 40f
                    axisMaximum = 180f
                    granularity = 20f
                    isGranularityEnabled = true
                }
            }
        },
        update = { chart ->
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            val dataSets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()

            if (showSys) dataSets.add(
                buildSet(systolic, chart.context.getString(R.string.chart_systolic), GColor.parseColor("#FFDD7A"), fill = true)
            )
            if (showDia) dataSets.add(
                buildSet(diastolic, chart.context.getString(R.string.chart_diastolic), GColor.parseColor("#7FE3C0"), fill = true)
            )
            if (showPulse) dataSets.add(
                buildSet(pulse, chart.context.getString(R.string.chart_pulse), GColor.parseColor("#A99CFF"), fill = false, dash = true)
                    .apply { axisDependency = YAxis.AxisDependency.RIGHT }
            )

            if (dataSets.isEmpty()) {
                chart.clear()
            } else {
                chart.data = LineData(dataSets)
            }
            chart.invalidate()
        }
    )
}

private fun buildSet(
    values: List<Float?>,
    label: String,
    color: Int,
    fill: Boolean,
    dash: Boolean = false
): LineDataSet {
    // 用 Float.NaN 表示缺失，MPAndroidChart 会跳过（需开启 setDraw... 或依赖默认）
    val entries = values.mapIndexedNotNull { i, v ->
        if (v == null) null else Entry(i.toFloat(), v)
    }
    return LineDataSet(entries, label).apply {
        this.color = color
        setCircleColor(color)
        lineWidth = 2.2f
        circleRadius = 3f
        setDrawCircleHole(false)
        valueTextColor = GColor.parseColor("#A8B0D8")
        valueTextSize = 8f
        valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
        }
        if (fill) {
            setDrawFilled(true)
            fillAlpha = 40
            fillColor = color
        } else {
            setDrawFilled(false)
        }
        if (dash) {
            enableDashedLine(8f, 4f, 0f)
        }
        mode = LineDataSet.Mode.CUBIC_BEZIER
        setDrawValues(false)
    }
}

/**
 * 血常规单指标趋势（含参考区间色带）。
 */
@Composable
fun LabLineChart(
    labels: List<String>,
    values: List<Float>,
    low: Float,
    high: Float,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setBackgroundColor(GColor.TRANSPARENT)
                legend.isEnabled = false
                setNoDataText(ctx.getString(R.string.chart_no_data))
                xAxis.apply {
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = GColor.parseColor("#A8B0D8")
                    textSize = 9f
                    granularity = 1f
                    isGranularityEnabled = true
                }
                axisLeft.apply {
                    textColor = GColor.parseColor("#A8B0D8")
                    textSize = 9f
                    gridColor = GColor.parseColor("#1AE8C25A")
                    removeAllLimitLines()
                    addLimitLine(LimitLine(high, ctx.getString(R.string.chart_ref_upper)).apply {
                        lineColor = GColor.parseColor("#8C7FE3C0")
                        lineWidth = 1f
                        enableDashedLine(6f, 4f, 0f)
                        textColor = GColor.parseColor("#8C7FE3C0")
                        textSize = 8f
                    })
                    addLimitLine(LimitLine(low, ctx.getString(R.string.chart_ref_lower)).apply {
                        lineColor = GColor.parseColor("#8C7FE3C0")
                        lineWidth = 1f
                        enableDashedLine(6f, 4f, 0f)
                        textColor = GColor.parseColor("#8C7FE3C0")
                        textSize = 8f
                    })
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val ds = LineDataSet(entries, chart.context.getString(R.string.chart_metric)).apply {
                color = GColor.parseColor("#FFDD7A")
                setCircleColor(GColor.parseColor("#FFDD7A"))
                lineWidth = 2.2f
                circleRadius = 4f
                setDrawCircleHole(false)
                setDrawFilled(true)
                fillAlpha = 30
                fillColor = GColor.parseColor("#FFDD7A")
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                // 异常点标红（circleColors 要求 MutableList<Int>）
                circleColors = values.map {
                    if (it < low || it > high) GColor.parseColor("#FF8080")
                    else GColor.parseColor("#FFDD7A")
                }.toMutableList()
            }
            chart.data = LineData(ds)
            chart.invalidate()
        }
    )
}
