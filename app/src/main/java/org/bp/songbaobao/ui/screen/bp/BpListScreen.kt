package org.bp.songbaobao.ui.screen.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.ui.components.classifyBp
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.EmptyHint
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*

@Composable
fun BpListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: BpViewModel = hiltViewModel()
) {
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    val records by vm.recordsFlow(from, to).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Gold,
                contentColor = Navy
            ) { Text("＋", fontWeight = FontWeight.Bold) }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium, color = Gold)
                }
                Text(
                    "全部血压记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright,
                    modifier = Modifier.weight(1f)
                )
                Text("${records.size} 条", style = MaterialTheme.typography.labelMedium, color = TextDim)
            }

            Spacer(Modifier.height(12.dp))

            // 日期筛选
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = from, onValueChange = { from = it },
                    label = "开始", modifier = Modifier.weight(1f)
                )
                AppTextField(
                    value = to, onValueChange = { to = it },
                    label = "结束", modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            if (records.isEmpty()) {
                EmptyHint("没有符合条件的记录")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records, key = { it.id }) { r ->
                        val lv = classifyBp(r.systolic, r.diastolic)
                        PanelCard {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${r.date} ${r.time}", fontWeight = FontWeight.SemiBold)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${r.systolic}/${r.diastolic}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(lv.name, style = MaterialTheme.typography.labelSmall) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = lv.color.copy(alpha = 0.25f),
                                                labelColor = lv.color
                                            )
                                        )
                                        if (r.pulse != null) {
                                            Text("心率 ${r.pulse}", style = MaterialTheme.typography.labelSmall, color = TextDim)
                                        }
                                    }
                                    if (r.note.isNotBlank()) {
                                        Text(r.note, style = MaterialTheme.typography.bodySmall, color = TextDim)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = { onEdit(r.id) }) {
                                        Text("编辑", color = Gold)
                                    }
                                    var confirm by remember { mutableStateOf(false) }
                                    TextButton(onClick = { confirm = true }) {
                                        Text("删除", color = DangerRed)
                                    }
                                    if (confirm) {
                                        ConfirmDelete(
                                            text = "确定删除 ${r.date} 的记录？",
                                            onDismiss = { confirm = false },
                                            onConfirm = { vm.delete(r.id) { confirm = false } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ConfirmDelete(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除", color = GoldBright) },
        text = { Text(text, color = TextMain) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = DangerRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextDim) }
        },
        containerColor = NavySoft,
        titleContentColor = GoldBright,
        textContentColor = TextMain
    )
}
