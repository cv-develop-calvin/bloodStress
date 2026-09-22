package org.bp.songbaobao.ui.screen.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.data.local.entity.LabReport
import org.bp.songbaobao.ui.components.CbcJudge
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*

/** 血常规录入/编辑。ocrValues 非空时预填并高亮 */
@Composable
fun LabFormScreen(
    reportId: Long? = null,
    ocrValues: Map<String, Double> = emptyMap(),
    rawText: String = "",
    onBack: () -> Unit,
    vm: LabViewModel = hiltViewModel()
) {
    var report by remember { mutableStateOf(vm.newReport(rawText)) }
    // 每个指标的原始输入串（用 SnapshotStateMap，直接改值即可刷新）
    val inputs = remember { mutableStateMapOf<String, String>() }
    var loaded by remember { mutableStateOf(reportId == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var prefillDone by remember { mutableStateOf(reportId != null) }

    LaunchedEffect(reportId) {
        if (reportId != null) {
            val r = vm.load(reportId)
            if (r != null) {
                report = r
                inputs.clear()
                CbcItems.items().forEach { item ->
                    inputs[item.key] = r.valueOf(item.key)?.let { fmt(it) } ?: ""
                }
            }
            loaded = true
        }
    }

    // OCR 预填
    LaunchedEffect(ocrValues) {
        if (!prefillDone && ocrValues.isNotEmpty()) {
            inputs.clear()
            CbcItems.items().forEach { item ->
                inputs[item.key] = ocrValues[item.key]?.let { fmt(it) } ?: ""
            }
            prefillDone = true
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Navy)) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if (reportId == null) "添加血常规" else "编辑血常规",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = GoldBright
        )

        if (ocrValues.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.15f))) {
                Text(
                    "已从照片识别到 ${ocrValues.size} 项（金色高亮），请核对后保存",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall, color = GoldBright
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = report.date, onValueChange = { report = report.copy(date = it) },
                label = "报告日期", modifier = Modifier.weight(1f))
            AppTextField(value = report.time, onValueChange = { report = report.copy(time = it) },
                label = "时间", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        AppTextField(value = report.hospital, onValueChange = { report = report.copy(hospital = it) },
            label = "医院 / 机构（可空）", placeholder = "如：市儿童医院")
        Spacer(Modifier.height(10.dp))
        AppTextField(value = report.note, onValueChange = { report = report.copy(note = it) },
            label = "备注（可空）", placeholder = "如：发热第 2 天复查")

        Spacer(Modifier.height(16.dp))

        // 指标输入
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                CbcItems.items().forEach { item ->
                    val text = inputs[item.key] ?: ""
                    val value = text.toDoubleOrNull()
                    val judge = CbcJudge.of(value, item)
                    val highlighted = ocrValues.containsKey(item.key)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (highlighted) Gold.copy(alpha = 0.12f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${fmt(item.low)}–${fmt(item.high)} ${item.unit}",
                                style = MaterialTheme.typography.labelSmall, color = TextDim
                            )
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { inputs[item.key] = it },
                            modifier = Modifier.width(96.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = PanelBorder,
                                focusedTextColor = TextMain,
                                unfocusedTextColor = TextMain
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            judge.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = judge.color,
                            modifier = Modifier.widthIn(min = 32.dp)
                        )
                    }
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    val parsed = mutableMapOf<String, Double?>()
                    CbcItems.items().forEach { item ->
                        parsed[item.key] = inputs[item.key]?.trim()
                            ?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                    }
                    if (parsed.values.all { it == null }) {
                        error = "请至少填写一项血常规指标"
                        return@GoldButton
                    }
                    if (report.date.isBlank()) {
                        error = "请填写报告日期"
                        return@GoldButton
                    }
                    val final = report.copy(
                        wbc = parsed["wbc"], rbc = parsed["rbc"], hgb = parsed["hgb"],
                        hct = parsed["hct"], mcv = parsed["mcv"], mch = parsed["mch"],
                        mchc = parsed["mchc"], plt = parsed["plt"],
                        lymPct = parsed["lymPct"], neutPct = parsed["neutPct"],
                        monoPct = parsed["monoPct"], eosPct = parsed["eosPct"],
                        crp = parsed["crp"],
                        rawText = rawText.ifBlank { report.rawText }
                    )
                    error = null
                    vm.save(final, onBack)
                },
                modifier = Modifier.weight(1f)
            ) { Text("保存", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
            ) { Text("取消") }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

@Composable
fun LabAddScreen(onBack: () -> Unit, vm: LabViewModel = hiltViewModel()) {
    // 从拍照页传递过来的 OCR 结果（读取后即清空，避免再次进入时重复预填）
    val (rawText, ocrValues) = remember {
        org.bp.songbaobao.domain.OcrResultHolder.consume()
    }
    LabFormScreen(
        reportId = null,
        ocrValues = ocrValues,
        rawText = rawText,
        onBack = onBack,
        vm = vm
    )
}

@Composable
fun LabEditScreen(onBack: () -> Unit, vm: LabViewModel = hiltViewModel()) {
    LabFormScreen(reportId = vm.editId, onBack = onBack, vm = vm)
}
