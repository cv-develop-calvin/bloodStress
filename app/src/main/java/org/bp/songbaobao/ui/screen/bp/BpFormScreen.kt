package org.bp.songbaobao.ui.screen.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.entity.BpRecord
import org.bp.songbaobao.domain.BpOcrResultHolder
import org.bp.songbaobao.ui.components.classifyBp
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.nowTimeStr
import org.bp.songbaobao.util.todayStr

/** 血压录入/编辑：新增与编辑共用，id==null 表示新增。
 *  @param prefill 由「拍照识别」页识别后带入的初始值（仅新增页使用）。 */
@Composable
fun BpFormScreen(
    recordId: Long? = null,
    onBack: () -> Unit,
    prefill: BpRecord? = null,
    vm: BpViewModel = hiltViewModel()
) {
    var record by remember(prefill) { mutableStateOf(prefill ?: vm.emptyRecord()) }
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
            stringResource(
                if (recordId == null) R.string.bp_form_title_add else R.string.bp_form_title_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        // 由照片识别带入时的核对提示
        if (prefill != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChartPulse.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.bp_ocr_prefill),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMain
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // 实时分级预览
        PanelCard {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.bp_form_current),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDim
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(level.nameRes)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = level.color.copy(alpha = 0.25f),
                            labelColor = level.color
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(level.adviceRes), style = MaterialTheme.typography.bodySmall,
                        color = TextDim, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = record.date,
                onValueChange = { record = record.copy(date = it) },
                label = stringResource(R.string.bp_form_date),
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                value = record.time,
                onValueChange = { record = record.copy(time = it) },
                label = stringResource(R.string.bp_form_time),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = record.systolic.toString(),
                onValueChange = { record = record.copy(systolic = it.toIntOrNull() ?: 0) },
                label = stringResource(R.string.bp_form_sys),
                modifier = Modifier.weight(1f)
            )
            AppTextField(
                value = record.diastolic.toString(),
                onValueChange = { record = record.copy(diastolic = it.toIntOrNull() ?: 0) },
                label = stringResource(R.string.bp_form_dia),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = record.pulse?.toString() ?: "",
            onValueChange = { record = record.copy(pulse = it.toIntOrNull()) },
            label = stringResource(R.string.bp_form_pulse)
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = record.note,
            onValueChange = { record = record.copy(note = it) },
            label = stringResource(R.string.bp_form_note),
            placeholder = stringResource(R.string.bp_form_note_hint)
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        val context = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    error = validate(context, record)
                    if (error == null) vm.save(record, onBack)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(R.string.action_save),
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
            ) { Text(stringResource(R.string.action_cancel)) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 校验：接收 context 以便按当前语言返回提示 */
private fun validate(
    context: android.content.Context,
    r: org.bp.songbaobao.data.local.entity.BpRecord
): String? {
    if (r.date.isBlank()) return context.getString(R.string.bp_err_date)
    if (r.systolic !in 60..300) return context.getString(R.string.bp_err_sys)
    if (r.diastolic !in 30..200) return context.getString(R.string.bp_err_dia)
    if (r.diastolic >= r.systolic) return context.getString(R.string.bp_err_dia_lt_sys)
    if (r.pulse != null && r.pulse!! !in 30..250) return context.getString(R.string.bp_err_pulse)
    return null
}

@Composable
fun BpAddScreen(onBack: () -> Unit) {
    val prefill = remember { BpOcrResultHolder.consume() }
    val rec = prefill?.let {
        BpRecord(
            date = todayStr(),
            time = nowTimeStr(),
            systolic = it.systolic ?: 0,
            diastolic = it.diastolic ?: 0,
            pulse = it.pulse,
            note = "",
            createdAt = nowStamp()
        )
    }
    BpFormScreen(recordId = null, onBack = onBack, prefill = rec)
}
