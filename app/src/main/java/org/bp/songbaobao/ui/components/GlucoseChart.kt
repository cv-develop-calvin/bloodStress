package org.bp.songbaobao.ui.components

import android.graphics.Color as GColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.bp.songbaobao.R

/**
 * 血糖趋势图（MPAndroidChart 通过 AndroidView 包装）。
 * 单一曲线，并绘制 3.9（偏低临界）与 11.1（偏高临界）参考线。
 */
@Composable
fun GlucoseLineChart(
    labels: List<String>,
    values: List<Float?>,
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
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = GColor.parseColor("#A8B0D8")
                    textSize = 9f
                    granularity = 1f
                    isGranularityEnabled = true
                }
                // 左轴（血糖 mmol/L）
                axisLeft.apply {
                    textColor = GColor.parseColor("#FFE9A8")
                    textSize = 9f
                    setDrawGridLines(true)
                    gridColor = GColor.parseColor("#1AE8C25A")
                    axisMinimum = 0f
                    axisMaximum = 20f
                    granularity = 2f
                    isGranularityEnabled = true
                    removeAllLimitLines()
                    addLimitLine(LimitLine(3.9f, ctx.getString(R.string.glucose_ref_low)).apply {
                        lineColor = GColor.parseColor("#8CFFC46E")
                        lineWidth = 1f
                        enableDashedLine(6f, 4f, 0f)
                        textColor = GColor.parseColor("#8CFFC46E")
                        textSize = 8f
                        labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    })
                    addLimitLine(LimitLine(11.1f, ctx.getString(R.string.glucose_ref_high)).apply {
                        lineColor = GColor.parseColor("#8CFF8080")
                        lineWidth = 1f
                        enableDashedLine(6f, 4f, 0f)
                        textColor = GColor.parseColor("#8CFF8080")
                        textSize = 8f
                        labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                    })
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            val entries = values.mapIndexedNotNull { i, v ->
                if (v == null) null else Entry(i.toFloat(), v)
            }
            if (entries.isEmpty()) {
                chart.clear()
            } else {
                val ds = LineDataSet(entries, chart.context.getString(R.string.glucose_value)).apply {
                    color = GColor.parseColor("#FFE9A8")
                    setCircleColor(GColor.parseColor("#FFE9A8"))
                    lineWidth = 2.2f
                    circleRadius = 3f
                    setDrawCircleHole(false)
                    valueTextColor = GColor.parseColor("#A8B0D8")
                    valueTextSize = 8f
                    setDrawFilled(true)
                    fillAlpha = 40
                    fillColor = GColor.parseColor("#FFE9A8")
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawValues(false)
                }
                chart.data = LineData(ds)
            }
            chart.invalidate()
        }
    )
}
