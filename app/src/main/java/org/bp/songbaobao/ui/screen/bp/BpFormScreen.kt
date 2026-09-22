package org.bp.songbaobao.ui.screen.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.domain.classifyBp
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*

/** 血压录入/编辑：新增与编辑共用，id==null 表示新增 */
@Composable
fun BpFormScreen(
    recordId: Long? = null,
    onBack: () -> Unit,
    vm: BpViewModel = hiltViewModel()
) {
    var record by remember { mutableStateOf(vm.emptyRecord()) }
    var loaded by remember { mutableStateOf(recordId == null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recordId) {
        if (recordId != null) {
            val r = vm.load(recordId)
            if (r != null) record = r
            loaded = true
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Navy)) { CircularProgressIndicator() }
        return
    }

    val sys = record.systolic
    val dia = record.diastolic
    val level = remember(sys, dia) { classifyBp(sys, dia) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if (recordId == null) "添加血压记录" else "编辑血压记录",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        // 实时分级预览
        PanelCard {
            Column(Modifier.padding(12.dp)) {
                Text("本次测量", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Spacer(Modifier.height(4.dp))
                Row {
                    AssistChip(
                        onClick = {},
                        label = { Text(level.name) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = level.color.copy(alpha = 0.25f),
                            labelColor = level.color
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(level.advice, style = MaterialTheme.typography.bodySmall,
                        color = TextDim, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = record.date,
                onValueChange = { record = record.copy(date = it) },
                label = "测量日期",
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                value = record.time,
                onValueChange = { record = record.copy(time = it) },
                label = "测量时间",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = record.systolic.toString(),
                onValueChange = { record = record.copy(systolic = it.toIntOrNull() ?: 0) },
                label = "收缩压(mmHg)",
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                value = record.diastolic.toString(),
                onValueChange = { record = record.copy(diastolic = it.toIntOrNull() ?: 0) },
                label = "舒张压(mmHg)",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = record.pulse?.toString() ?: "",
            onValueChange = { record = record.copy(pulse = it.toIntOrNull()) },
            label = "心率(次/分，选填)"
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = record.note,
            onValueChange = { record = record.copy(note = it) },
            label = "备注（选填）",
            placeholder = "如：晨起、服药后、运动后"
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    error = validate(record)
                    if (error == null) vm.save(record, onBack)
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

private fun validate(r: org.bp.songbaobao.data.local.entity.BpRecord): String? {
    if (r.date.isBlank()) return "请选择测量日期"
    if (r.systolic !in 60..300) return "收缩压应在 60-300 之间"
    if (r.diastolic !in 30..200) return "舒张压应在 30-200 之间"
    if (r.diastolic >= r.systolic) return "舒张压应小于收缩压"
    if (r.pulse != null && r.pulse!! !in 30..250) return "心率应在 30-250 之间"
    return null
}

@Composable
fun BpAddScreen(onBack: () -> Unit) {
    BpFormScreen(recordId = null, onBack = onBack)
}
