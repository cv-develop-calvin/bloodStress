package org.bp.songbaobao.ui.screen.med

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*

/** 药品新增/编辑：id==null 为新增 */
@Composable
fun MedFormScreen(
    medId: Long? = null,
    onBack: () -> Unit,
    vm: MedViewModel = hiltViewModel()
) {
    var med by remember { mutableStateOf(vm.newMed()) }
    var loaded by remember { mutableStateOf(medId == null) }
    var times by remember { mutableStateOf(listOf("08:00")) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medId) {
        if (medId != null) {
            val m = vm.load(medId)
            if (m != null) {
                med = m
                times = m.times.split(",").filter { it.isNotBlank() }.ifEmpty { listOf("08:00") }
            }
            loaded = true
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Navy)) { CircularProgressIndicator() }
        return
    }

    // 频次用资源 id 表示，显示文本随语言切换
    val freqIds = listOf(
        R.string.freq_daily_1,
        R.string.freq_daily_2,
        R.string.freq_daily_3,
        R.string.freq_every_other,
        R.string.freq_weekly,
        R.string.freq_as_needed
    )

    AppBackground {
    // 以下必须在 Composable 作用域内取（stringResource 只能在 @Composable 中调用）
    val asNeededText = stringResource(R.string.freq_as_needed)
    val freqs = freqIds.map { it to stringResource(it) }
    // med.freq 存的是文案原文；若当前语言下找不到匹配项就保留原值
    val currentFreqText = freqs.firstOrNull { it.second == med.freq }?.second ?: med.freq
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(
                if (medId == null) R.string.title_add_med else R.string.title_edit_med
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        AppTextField(value = med.name, onValueChange = { med = med.copy(name = it) },
            label = stringResource(R.string.med_field_name),
            placeholder = stringResource(R.string.med_field_name_hint))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = med.dosage, onValueChange = { med = med.copy(dosage = it) },
                label = stringResource(R.string.med_field_dosage),
                modifier = Modifier.weight(1f))
            AppTextField(value = med.unit, onValueChange = { med = med.copy(unit = it) },
                label = stringResource(R.string.med_field_unit),
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        // 频次
        Text(
            stringResource(R.string.med_field_freq),
            style = MaterialTheme.typography.labelMedium, color = TextDim
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            freqs.forEach { (_, text) ->
                FilterChip(
                    selected = currentFreqText == text,
                    onClick = { med = med.copy(freq = text) },
                    label = { Text(text, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold.copy(alpha = 0.25f),
                        selectedLabelColor = GoldBright,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        labelColor = TextDim
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // 服药时间（可增减）
        if (med.freq != asNeededText) {
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.med_field_times),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextDim, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { times = times + "08:00" }) {
                            Text(stringResource(R.string.med_add_time), color = Gold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    times.forEachIndexed { i, t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppTextField(
                                value = t,
                                onValueChange = { v ->
                                    times = times.toMutableList().also { it[i] = v }
                                },
                                label = stringResource(R.string.med_time_index, i),
                                modifier = Modifier.weight(1f)
                            )
                            if (times.size > 1) {
                                IconButton(onClick = { times = times.filterIndexed { idx, _ -> idx != i } }) {
                                    Text("×", style = MaterialTheme.typography.titleMedium, color = DangerRed)
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.med_time_format_hint),
                        style = MaterialTheme.typography.labelSmall, color = TextDim
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = med.startDate, onValueChange = { med = med.copy(startDate = it) },
                label = stringResource(R.string.med_field_start),
                modifier = Modifier.weight(1f))
            AppTextField(value = med.endDate, onValueChange = { med = med.copy(endDate = it) },
                label = stringResource(R.string.med_field_end),
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        AppTextField(value = med.note, onValueChange = { med = med.copy(note = it) },
            label = stringResource(R.string.med_field_note),
            placeholder = stringResource(R.string.med_field_note_hint))
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.med_remind_enabled),
                color = TextMain, modifier = Modifier.weight(1f)
            )
            Switch(checked = med.active, onCheckedChange = { med = med.copy(active = it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = GoldDeep))
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        val context = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    val normalized = times.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                    error = validate(context, med, normalized)
                    if (error == null) {
                        vm.save(med.copy(times = normalized.joinToString(",")), onBack)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
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
}

private fun validate(
    context: android.content.Context,
    med: org.bp.songbaobao.data.local.entity.Medication,
    times: List<String>
): String? {
    if (med.name.isBlank()) return context.getString(R.string.med_err_name_required)
    if (med.freq != context.getString(R.string.freq_as_needed) && times.isEmpty()) {
        return context.getString(R.string.med_err_time_required)
    }
    times.forEach {
        if (!Regex("\\d{2}:\\d{2}").matches(it)) {
            return context.getString(R.string.med_err_time_format, it)
        }
    }
    if (med.endDate.isNotBlank() && med.endDate < med.startDate) {
        return context.getString(R.string.med_err_end_before_start)
    }
    return null
}

@Composable
fun MedAddScreen(onBack: () -> Unit) {
    MedFormScreen(medId = null, onBack = onBack)
}

@Composable
fun MedEditScreen(onBack: () -> Unit, vm: MedViewModel = hiltViewModel()) {
    MedFormScreen(medId = vm.editId, onBack = onBack, vm = vm)
}
