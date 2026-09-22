package org.bp.songbaobao.ui.screen.med

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    val freqs = listOf("每日 1 次", "每日 2 次", "每日 3 次", "隔日 1 次", "每周 1 次", "按需服用")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if (medId == null) "添加药品" else "编辑药品",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        AppTextField(value = med.name, onValueChange = { med = med.copy(name = it) },
            label = "药品名称", placeholder = "如：氨氯地平")
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = med.dosage, onValueChange = { med = med.copy(dosage = it) },
                label = "剂量", modifier = Modifier.weight(1f))
            AppTextField(value = med.unit, onValueChange = { med = med.copy(unit = it) },
                label = "单位", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        // 频次
        Text("服药频次", style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            freqs.forEach { f ->
                FilterChip(
                    selected = med.freq == f,
                    onClick = { med = med.copy(freq = f) },
                    label = { Text(f, style = MaterialTheme.typography.labelSmall) },
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
        if (med.freq != "按需服用") {
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("服药时间", style = MaterialTheme.typography.labelMedium,
                            color = TextDim, modifier = Modifier.weight(1f))
                        TextButton(onClick = { times = times + "08:00" }) {
                            Text("＋ 添加", color = Gold)
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
                                label = "时间 $i",
                                modifier = Modifier.weight(1f)
                            )
                            if (times.size > 1) {
                                IconButton(onClick = { times = times.filterIndexed { idx, _ -> idx != i } }) {
                                    Text("×", style = MaterialTheme.typography.titleMedium, color = DangerRed)
                                }
                            }
                        }
                    }
                    Text("格式 HH:mm，如 08:00",
                        style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = med.startDate, onValueChange = { med = med.copy(startDate = it) },
                label = "开始日期", modifier = Modifier.weight(1f))
            AppTextField(value = med.endDate, onValueChange = { med = med.copy(endDate = it) },
                label = "结束日期（可空）", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        AppTextField(value = med.note, onValueChange = { med = med.copy(note = it) },
            label = "备注（可空）", placeholder = "如：饭后服用")
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("启用提醒", color = TextMain, modifier = Modifier.weight(1f))
            Switch(checked = med.active, onCheckedChange = { med = med.copy(active = it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = GoldDeep))
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    val normalized = times.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                    error = validate(med, normalized)
                    if (error == null) {
                        vm.save(med.copy(times = normalized.joinToString(",")), onBack)
                    }
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

private fun validate(med: org.bp.songbaobao.data.local.entity.Medication, times: List<String>): String? {
    if (med.name.isBlank()) return "请填写药品名称"
    if (med.freq != "按需服用" && times.isEmpty()) return "请至少设置一个服药时间"
    times.forEach {
        if (!Regex("\\d{2}:\\d{2}").matches(it)) return "时间格式不正确：$it（应为 HH:mm）"
    }
    if (med.endDate.isNotBlank() && med.endDate < med.startDate) return "结束日期不能早于开始日期"
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
