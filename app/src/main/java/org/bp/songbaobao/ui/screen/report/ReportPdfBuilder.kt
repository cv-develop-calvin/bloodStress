package org.bp.songbaobao.ui.screen.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.dao.MedLogWithMed
import org.bp.songbaobao.data.local.entity.BpRecord
import org.bp.songbaobao.data.local.entity.GlucoseRecord
import org.bp.songbaobao.data.local.entity.LabReport
import org.bp.songbaobao.ui.components.classifyBp
import org.bp.songbaobao.ui.components.classifyGlucose
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.data.repository.Adherence
import org.bp.songbaobao.ui.components.BpLevel
import org.bp.songbaobao.ui.components.GlucoseLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地生成血压/血糖趋势 PDF 报告（医生可读版）。
 * 全程离线、不联网，符合「数据不出手机」。
 */
object ReportPdfBuilder {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36
    private const val CHART_W = PAGE_W - MARGIN * 2

    fun build(
        context: Context,
        data: ReportData,
        bpBitmap: Bitmap? = null,
        glucoseBitmap: Bitmap? = null
    ): File {
        val dir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "health_report_${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        var canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensure(space: Int) {
            if (y + space > PAGE_H - 50) newPage()
        }

        fun text(paint: Paint, str: String, x: Int = MARGIN, dy: Int) {
            canvas.drawText(str, x.toFloat(), (y + dy).toFloat(), paint)
            y += dy
        }

        val title = Paint().apply {
            color = Color.BLACK; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
        }
        val heading = Paint().apply {
            color = Color.BLACK; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        }
        val body = TextPaint().apply {
            color = Color.DKGRAY; textSize = 11f; typeface = Typeface.DEFAULT
        }
        val small = TextPaint().apply {
            color = Color.GRAY; textSize = 9f; typeface = Typeface.DEFAULT
        }
        val value = Paint().apply {
            color = Color.BLACK; textSize = 11f; typeface = Typeface.DEFAULT
        }

        // 标题
        val titleStr = context.getString(R.string.app_name) + " " + context.getString(R.string.report_title)
        canvas.drawText(titleStr, MARGIN.toFloat(), (y + 18).toFloat(), title)
        y += 30
        text(small,
            context.getString(R.string.report_summary, "${data.rangeFrom} ~ ${data.rangeTo}"),
            dy = 14)
        y += 6

        val hasAny = data.bp.isNotEmpty() || data.glucose.isNotEmpty() || data.lab != null

        // 一、血压
        if (data.bp.isNotEmpty()) {
            ensure(280)
            canvas.drawText(context.getString(R.string.report_section_bp), MARGIN.toFloat(), (y + 14).toFloat(), heading)
            y += 22
            val bmp = bpBitmap
            if (bmp != null) {
                canvas.drawBitmap(bmp, MARGIN.toFloat(), y.toFloat(), null)
                y += bmp.height + 8
            }
            val avgSys = data.bp.map { it.systolic }.average().toInt()
            val avgDia = data.bp.map { it.diastolic }.average().toInt()
            text(value, context.getString(R.string.report_avg, "$avgSys", "$avgDia"), dy = 16)
            val lvStr = data.bpLevelCount.entries.joinToString("，") {
                bpLevelLabel(context, it.key) + " ${it.value} 次"
            }
            text(body, context.getString(R.string.report_level_stat) + "：" + lvStr, dy = 16)
        } else {
            ensure(40)
            canvas.drawText(context.getString(R.string.report_section_bp), MARGIN.toFloat(), (y + 14).toFloat(), heading)
            y += 22
            text(small, context.getString(R.string.report_empty), dy = 14)
        }

        // 二、血糖
        y += 10
        if (data.glucose.isNotEmpty()) {
            ensure(280)
            canvas.drawText(context.getString(R.string.report_section_glucose), MARGIN.toFloat(), (y + 14).toFloat(), heading)
            y += 22
            val bmp = glucoseBitmap
            if (bmp != null) {
                canvas.drawBitmap(bmp, MARGIN.toFloat(), y.toFloat(), null)
                y += bmp.height + 8
            }
            val rate = if (data.glucoseTotal > 0) (data.glucoseOkCount * 100 / data.glucoseTotal) else 0
            text(value, context.getString(R.string.report_rate, rate), dy = 16)
            text(body, "共 ${data.glucoseTotal} 条记录", dy = 16)
        } else {
            ensure(40)
            canvas.drawText(context.getString(R.string.report_section_glucose), MARGIN.toFloat(), (y + 14).toFloat(), heading)
            y += 22
            text(small, context.getString(R.string.report_empty), dy = 14)
        }

        // 三、血常规异常项
        y += 10
        ensure(60)
        canvas.drawText(context.getString(R.string.report_section_lab), MARGIN.toFloat(), (y + 14).toFloat(), heading)
        y += 22
        if (data.lab != null && data.labAbnormal.isNotEmpty()) {
            data.labAbnormal.forEach { (key, detail) ->
                ensure(18)
                val label = runCatching { context.getString(CbcItems.get(key).labelRes) }.getOrDefault(key)
                text(body, "• $label：$detail", dy = 16)
            }
        } else {
            text(small, context.getString(R.string.report_all_normal), dy = 14)
        }

        // 四、用药依从性
        y += 10
        ensure(60)
        canvas.drawText(context.getString(R.string.report_section_med), MARGIN.toFloat(), (y + 14).toFloat(), heading)
        y += 22
        val adh = data.adherence
        if (adh != null) {
            text(value,
                context.getString(R.string.report_compliance) + "：${adh.rate}%（近30天 已服 ${adh.taken}/${adh.expected}）",
                dy = 16)
        } else {
            text(small, context.getString(R.string.report_empty), dy = 14)
        }
        val missed = data.medLogs.filter { it.status == "missed" }
        if (missed.isNotEmpty()) {
            text(body, "漏服明细（${missed.size} 次）：", dy = 16)
            missed.take(15).forEach {
                ensure(16)
                text(small, "  - ${it.date} ${it.time} ${it.medName}", dy = 14)
            }
        }

        // 五、异常事件
        y += 10
        val alerts = buildAlerts(context, data)
        if (alerts.isNotEmpty()) {
            ensure(60)
            canvas.drawText(context.getString(R.string.report_section_alert), MARGIN.toFloat(), (y + 14).toFloat(), heading)
            y += 22
            alerts.forEach {
                ensure(16)
                text(body, "• $it", dy = 16)
            }
        }

        // 页脚：免责声明
        ensure(60)
        y += 12
        val disclaim = context.getString(R.string.report_disclaimer)
        val disclaimWrapped = wrapText(disclaim, body, CHART_W)
        disclaimWrapped.forEach {
            ensure(14)
            canvas.drawText(it, MARGIN.toFloat(), (y + 11).toFloat(), small)
            y += 13
        }
        text(small,
            "生成时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())}　版本：${versionName(context)}",
            dy = 13)

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun bpLevelLabel(context: Context, level: BpLevel): String = when (level) {
        BpLevel.OPTIMAL -> context.getString(R.string.bp_level_optimal)
        BpLevel.NORMAL -> context.getString(R.string.bp_level_normal)
        BpLevel.ELEVATED -> context.getString(R.string.bp_level_elevated)
        BpLevel.STAGE1 -> context.getString(R.string.bp_level_stage1)
        BpLevel.STAGE2 -> context.getString(R.string.bp_level_stage2)
        BpLevel.CRISIS -> context.getString(R.string.bp_level_crisis)
    }

    private fun buildAlerts(context: Context, data: ReportData): List<String> {
        val out = mutableListOf<String>()
        data.bp.filter { classifyBp(it.systolic, it.diastolic) == BpLevel.CRISIS }
            .takeLast(10)
            .forEach { out.add("${it.date} ${it.time} 血压 ${it.systolic}/${it.diastolic} 已达危象水平，建议尽快就医") }
        data.glucose.filter { it.value >= 16.7f || it.value <= 3.9f }
            .takeLast(10)
            .forEach { out.add("${it.date} ${it.time} 血糖 ${it.value} mmol/L 属危急值，建议尽快就医") }
        return out
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val result = mutableListOf<String>()
        var line = ""
        text.forEach { ch ->
            val test = line + ch
            if (paint.measureText(test) > maxWidth) {
                result.add(line)
                line = ch.toString()
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) result.add(line)
        return result
    }

    private fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")
}

/** 报告所需数据（由 ReportViewModel 组装） */
data class ReportData(
    val rangeFrom: String,
    val rangeTo: String,
    val bp: List<BpRecord> = emptyList(),
    val glucose: List<GlucoseRecord> = emptyList(),
    val lab: LabReport? = null,
    val adherence: Adherence? = null,
    val medLogs: List<MedLogWithMed> = emptyList(),
    val bpLevelCount: Map<BpLevel, Int> = emptyMap(),
    val glucoseOkCount: Int = 0,
    val glucoseTotal: Int = 0,
    val labAbnormal: List<Pair<String, String>> = emptyList()
)
