package org.bp.songbaobao.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.entity.BpRecord
import org.bp.songbaobao.data.local.entity.GlucoseRecord

/**
 * 离屏把趋势图渲染成 Bitmap，嵌进 PDF 报告。
 * 样式与屏幕上的 BpChart / GlucoseChart 保持一致，但纯本地生成、不依赖界面。
 */

private const val SYS_COLOR = 0xFFD32F2F.toInt()   // 红：收缩压
private const val DIA_COLOR = 0xFF2196F3.toInt()   // 蓝：舒张压
private const val PULSE_COLOR = 0xFFFF9800.toInt() // 橙：脉搏
private const val GLU_COLOR = 0xFFC8961E.toInt()   // 金：血糖

fun renderBpLineChart(
    context: Context,
    width: Int,
    height: Int,
    records: List<BpRecord>,
    showSys: Boolean = true,
    showDia: Boolean = true,
    showPulse: Boolean = true
): Bitmap {
    val sorted = records.sortedBy { it.date + it.time }
    val labels = sorted.map { it.date.substring(5) } // MM-DD
    val chart = LineChart(context).apply {
        layoutParams = ViewGroup.LayoutParams(width, height)
        description.isEnabled = false
        setTouchEnabled(false)
        setScaleEnabled(false)
        setPinchZoom(false)
        legend.isEnabled = true
        legend.textColor = android.graphics.Color.DKGRAY
        legend.textSize = 11f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = android.graphics.Color.DKGRAY
        xAxis.textSize = 10f
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = -45f
        axisLeft.axisMinimum = 40f
        axisLeft.axisMaximum = 200f
        axisLeft.textColor = android.graphics.Color.DKGRAY
        axisLeft.textSize = 10f
        axisRight.isEnabled = false
        setExtraOffsets(10f, 10f, 10f, 28f)
    }

    val sets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
    if (showSys) {
        val e = sorted.mapIndexedNotNull { i, r -> Entry(i.toFloat(), r.systolic.toFloat()) }
        sets.add(LineDataSet(e, "收缩压").apply {
            color = SYS_COLOR; setCircleColor(color); circleRadius = 2f; lineWidth = 2f
            mode = LineDataSet.Mode.LINEAR; setDrawValues(false)
            setDrawCircles(e.size <= 60); valueTextSize = 9f
        })
    }
    if (showDia) {
        val e = sorted.mapIndexedNotNull { i, r -> Entry(i.toFloat(), r.diastolic.toFloat()) }
        sets.add(LineDataSet(e, "舒张压").apply {
            color = DIA_COLOR; setCircleColor(color); circleRadius = 2f; lineWidth = 2f
            mode = LineDataSet.Mode.LINEAR; setDrawValues(false)
            setDrawCircles(e.size <= 60); valueTextSize = 9f
        })
    }
    if (showPulse) {
        val e = sorted.mapIndexedNotNull { i, r -> r.pulse?.toFloat()?.let { Entry(i.toFloat(), it) } }
        if (e.isNotEmpty()) {
            sets.add(LineDataSet(e, context.getString(R.string.bp_pulse)).apply {
                color = PULSE_COLOR; setCircleColor(color); circleRadius = 2f; lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR; setDrawValues(false)
                setDrawCircles(e.size <= 60); valueTextSize = 9f
            })
        }
    }
    chart.data = LineData(sets)
    return chart.toBitmap(width, height)
}

fun renderGlucoseLineChart(
    context: Context,
    width: Int,
    height: Int,
    records: List<GlucoseRecord>
): Bitmap {
    val sorted = records.sortedBy { it.date + it.time }
    val labels = sorted.map { it.date.substring(5) }
    val chart = LineChart(context).apply {
        layoutParams = ViewGroup.LayoutParams(width, height)
        description.isEnabled = false
        setTouchEnabled(false)
        setScaleEnabled(false)
        setPinchZoom(false)
        legend.isEnabled = false
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = android.graphics.Color.DKGRAY
        xAxis.textSize = 10f
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = -45f
        axisLeft.axisMinimum = 0f
        axisLeft.axisMaximum = 30f
        axisLeft.textColor = android.graphics.Color.DKGRAY
        axisLeft.textSize = 10f
        axisRight.isEnabled = false
        setExtraOffsets(10f, 10f, 10f, 28f)
    }
    val entries = sorted.mapIndexedNotNull { i, r -> Entry(i.toFloat(), r.value) }
    val set = LineDataSet(entries, context.getString(R.string.nav_glucose)).apply {
        color = GLU_COLOR; setCircleColor(color); circleRadius = 2f; lineWidth = 2f
        mode = LineDataSet.Mode.LINEAR; setDrawValues(false)
        setDrawCircles(entries.size <= 60); valueTextSize = 9f
    }
    chart.data = LineData(set)
    return chart.toBitmap(width, height)
}

/** 离屏测量 + 绘制到 Bitmap（不依赖界面线程） */
private fun LineChart.toBitmap(width: Int, height: Int): Bitmap {
    measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    layout(0, 0, width, height)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    draw(Canvas(bmp))
    return bmp
}
