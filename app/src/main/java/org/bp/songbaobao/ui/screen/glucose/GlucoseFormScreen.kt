package org.bp.songbaobao.ui.screen.glucose

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.entity.GlucoseRecord
import org.bp.songbaobao.domain.GlucoseOcrResultHolder
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.nowTimeStr
import org.bp.songbaobao.util.todayStr

/** 血糖录入/编辑：新增与编辑共用，recordId==null 表示新增。
 *  @param prefill 由「拍照识别」页识别后带入的初始值（仅新增页使用）。 */
@Composable
fun GlucoseFormScreen(
    recordId: Long? = null,
    onBack: () -> Unit,
    prefill: GlucoseRecord? = null,
    vm: GlucoseViewModel = hiltViewModel()
) {
    // 每个字段用独立的 rememberSaveable 文本状态作为显示唯一来源，
    // 不受父级重组 / 软键盘弹出导致 record 被重置的影响，避免预填值被清空。
    val init = prefill ?: vm.emptyRecord()
    var dateText by rememberSaveable { mutableStateOf(init.date) }
    var timeText by rememberSaveable { mutableStateOf(init.time) }
    var valueText by rememberSaveable { mutableStateOf(formatValue(init.value)) }
    var noteText by rememberSaveable { mutableStateOf(init.note) }
    var contextKey by rememberSaveable { mutableStateOf(init.context) }
    var createdAt by rememberSaveable { mutableStateOf(init.createdAt) }
    var loaded by rememberSaveable { mutableStateOf(recordId == null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recordId) {
        if (recordId != null) {
            val r = vm.load(recordId)
            if (r != null) {
                dateText = r.date
                timeText = r.time
                valueText = formatValue(r.value)
                noteText = r.note
                contextKey = r.context
                createdAt = r.createdAt
            }
            loaded = true
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Navy)) { CircularProgressIndicator() }
        return
    }

    val ctx = GlucoseContext.fromKey(contextKey)
    val parsedValue = valueText.toFloatOrNull() ?: 0f
    val level = remember(valueText, contextKey) { classifyGlucose(parsedValue, ctx) }

    val record = GlucoseRecord(
        id = recordId ?: 0,
        date = dateText,
        time = timeText,
        value = parsedValue,
        context = contextKey,
        note = noteText,
        createdAt = createdAt.ifBlank { nowStamp() }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(
                if (recordId == null) R.string.glucose_form_title_add else R.string.glucose_form_title_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        if (prefill != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChartPulse.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.glucose_ocr_prefill),
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
                    stringResource(R.string.glucose_form_current),
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
                    Text(
                        stringResource(level.adviceRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 测量时段
        Text(
            stringResource(R.string.glucose_form_context),
            style = MaterialTheme.typography.labelMedium,
            color = TextDim
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlucoseContext.list().forEach { c ->
                FilterChip(
                    selected = c.key == contextKey,
                    onClick = { contextKey = c.key },
                    label = { Text(stringResource(c.labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold.copy(alpha = 0.25f),
                        selectedLabelColor = GoldBright,
                        containerColor = Color.Transparent,
                        labelColor = TextDim
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = stringResource(R.string.glucose_form_date),
                modifier = Modifier.weight(1f),
                clearOnFocus = true
            )
            AppTextField(
                value = timeText,
                onValueChange = { timeText = it },
                label = stringResource(R.string.glucose_form_time),
                modifier = Modifier.weight(1f),
                clearOnFocus = true
            )
        }
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = valueText,
            onValueChange = {
                // 仅允许数字与最多一个小数点
                val filtered = it.filter { c -> c.isDigit() || c == '.' }
                val dots = filtered.count { c -> c == '.' }
                valueText = if (dots <= 1) filtered.take(6) else filtered.replaceFirst(".", "")
            },
            label = stringResource(R.string.glucose_form_value),
            placeholder = stringResource(R.string.glucose_form_value_hint),
            clearOnFocus = true
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = stringResource(R.string.glucose_form_note),
            placeholder = stringResource(R.string.glucose_form_note_hint),
            clearOnFocus = true
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

private fun formatValue(v: Float): String = if (v > 0f) "%.1f".format(v) else ""

/** 校验：接收 context 以便按当前语言返回提示 */
private fun validate(
    context: android.content.Context,
    r: GlucoseRecord
): String? {
    if (r.date.isBlank()) return context.getString(R.string.glucose_err_date)
    if (r.value !in 1.0f..50.0f) return context.getString(R.string.glucose_err_value)
    return null
}

@Composable
fun GlucoseAddScreen(onBack: () -> Unit) {
    // 消费一次识别结果，并稳定持有，避免父级重组导致 prefill 引用变化而重置表单
    val scanned = remember { GlucoseOcrResultHolder.consume() }
    val rec = remember(scanned) {
        scanned?.let {
            GlucoseRecord(
                date = todayStr(),
                time = nowTimeStr(),
                value = it.value ?: 5.5f,
                context = GlucoseContext.FASTING.key,
                note = "",
                createdAt = nowStamp()
            )
        }
    }
    GlucoseFormScreen(recordId = null, onBack = onBack, prefill = rec)
}
