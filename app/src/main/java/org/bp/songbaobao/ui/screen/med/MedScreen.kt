package org.bp.songbaobao.ui.screen.med

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.parseTimes

@Composable
fun MedScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: MedViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Gold,
                contentColor = Navy
            ) { Text("＋", fontWeight = FontWeight.Bold) }
        }
    ) { padding ->
      AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 概览
            HeroCard {
                ZodiacChip("用药管理 · MEDICATION")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${state.adherence?.rate ?: "--"}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if ((state.adherence?.rate ?: 100) < 80) DangerRed else SuccessGreen
                    )
                    Text("%", color = TextDim)
                    Spacer(Modifier.width(8.dp))
                    Text("近 7 天依从性", style = MaterialTheme.typography.labelMedium, color = TextDim)
                }
                Text(
                    "打卡 ${state.adherence?.taken ?: 0} / ${state.adherence?.expected ?: 0} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }

            Spacer(Modifier.height(12.dp))

            // 待服药
            if (state.pending.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarnAmber.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⏰ 该吃药了", fontWeight = FontWeight.Bold, color = WarnAmber)
                        Spacer(Modifier.height(6.dp))
                        state.pending.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.med.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${p.med.dosage}${p.med.unit} · ${p.slot}",
                                        style = MaterialTheme.typography.bodySmall, color = TextDim
                                    )
                                }
                                GoldButton(onClick = { vm.toggleTaken(p.med.id, p.slot) }) {
                                    Text("打卡")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 药品列表
            SectionTitle("药品（${state.meds.size}）")

            if (state.meds.isEmpty()) {
                EmptyHint("还没有添加药品", "点击右下角＋添加")
            } else {
                state.meds.forEach { med ->
                    val slots = parseTimes(med.times)
                    PanelCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(med.name, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, color = GoldBright)
                                    Text(
                                        "${med.dosage}${med.unit} · ${med.freq}",
                                        style = MaterialTheme.typography.bodySmall, color = TextDim
                                    )
                                    if (med.startDate.isNotBlank()) {
                                        Text(
                                            "起 ${med.startDate}${if (med.endDate.isNotBlank()) " · 止 ${med.endDate}" else ""}",
                                            style = MaterialTheme.typography.labelSmall, color = TextDim
                                        )
                                    }
                                    if (med.note.isNotBlank()) {
                                        Text(med.note, style = MaterialTheme.typography.bodySmall, color = TextDim)
                                    }
                                }
                                Switch(
                                    checked = med.active,
                                    onCheckedChange = { vm.setActive(med, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Gold,
                                        checkedTrackColor = GoldDeep
                                    )
                                )
                            }

                            if (slots.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    slots.forEach { slot ->
                                        val taken = (med.id to slot) in state.takenSlots
                                        FilterChip(
                                            selected = taken,
                                            onClick = { vm.toggleTaken(med.id, slot) },
                                            label = { Text(if (taken) "✓ $slot" else slot) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SuccessGreen.copy(alpha = 0.25f),
                                                selectedLabelColor = SuccessGreen,
                                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                                labelColor = TextDim
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onEdit(med.id) }) { Text("编辑", color = Gold) }
                                var showBuy by remember { mutableStateOf(false) }
                                TextButton(onClick = { showBuy = true }) {
                                    Text("购买", color = SuccessGreen)
                                }
                                var confirm by remember { mutableStateOf(false) }
                                TextButton(onClick = { confirm = true }) { Text("删除", color = DangerRed) }
                                if (confirm) {
                                    org.bp.songbaobao.ui.screen.bp.ConfirmDelete(
                                        text = "删除「${med.name}」及其服药记录？",
                                        onDismiss = { confirm = false },
                                        onConfirm = { vm.delete(med); confirm = false }
                                    )
                                }
                                if (showBuy) {
                                    BuyChannelDialog(
                                        name = med.name,
                                        dosage = med.dosage,
                                        onDismiss = { showBuy = false },
                                        onPick = { channel ->
                                            val ok = org.bp.songbaobao.util.MedPurchase.buyWith(
                                                context = context,
                                                channel = channel,
                                                name = med.name,
                                                dosage = med.dosage
                                            )
                                            if (!ok) {
                                                Toast.makeText(
                                                    context,
                                                    "无法打开${channel.name}，已复制药名，可粘贴到该 App 搜索",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                org.bp.songbaobao.util.MedPurchase.copyName(
                                                    context,
                                                    org.bp.songbaobao.util.MedPurchase
                                                        .keywordOf(med.name, med.dosage)
                                                )
                                            }
                                            showBuy = false
                                        },
                                        onCopy = {
                                            org.bp.songbaobao.util.MedPurchase.copyName(
                                                context,
                                                org.bp.songbaobao.util.MedPurchase
                                                    .keywordOf(med.name, med.dosage)
                                            )
                                            showBuy = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 服药流水
            if (state.logs.isNotEmpty()) {
                PanelCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionTitle("服药记录")
                        state.logs.take(10).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.medName ?: "已删除药品", fontWeight = FontWeight.SemiBold)
                                    Text("${log.date} ${log.time}", style = MaterialTheme.typography.labelSmall, color = TextDim)
                                }
                                if (log.medDosage != null) {
                                    Text(
                                        "${log.medDosage}${log.medUnit ?: ""}",
                                        style = MaterialTheme.typography.bodySmall, color = TextDim
                                    )
                                }
                            }
                            HorizontalDivider(color = DividerGold.copy(alpha = 0.35f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
      }
    }
}

/**
 * 购买渠道选择。
 *
 * 之所以让用户选而不是直接跳转：电商 H5 地址会随时间失效，
 * 单一渠道失效就等于功能不可用。这里给出多个渠道，
 * 并提供「复制药名」兜底——即使全部渠道都打不开，
 * 用户也能粘贴药名到自己常用的购药 App 里搜索。
 */
@Composable
private fun BuyChannelDialog(
    name: String,
    dosage: String,
    onDismiss: () -> Unit,
    onPick: (org.bp.songbaobao.util.MedPurchase.Channel) -> Unit,
    onCopy: () -> Unit
) {
    val keyword = org.bp.songbaobao.util.MedPurchase.keywordOf(name, dosage)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySoft,
        title = {
            Text(
                "购买「$name」",
                fontWeight = FontWeight.Bold,
                color = GoldBright
            )
        },
        text = {
            Column {
                Text(
                    "将按「$keyword」搜索，请选择购药渠道：",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
                Spacer(Modifier.height(10.dp))
                org.bp.songbaobao.util.MedPurchase.channels.forEach { ch ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(ch) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            ch.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldBright
                        )
                        Text(
                            ch.desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim
                        )
                    }
                    HorizontalDivider(color = DividerGold.copy(alpha = 0.25f))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "若渠道打不开或链接失效，可复制药名后自行在购药 App 中搜索。",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) { Text("复制药名", color = Gold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextDim) }
        }
    )
}
