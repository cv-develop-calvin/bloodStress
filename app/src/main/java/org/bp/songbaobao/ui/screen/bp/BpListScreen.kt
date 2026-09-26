package org.bp.songbaobao.ui.screen.bp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.AppBackground
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
      AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium, color = Gold)
                }
                Text(
                    stringResource(R.string.title_all_records),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright,
                    modifier = Modifier.weight(1f)
                )
                Text(stringResource(R.string.bp_records_count, records.size), style = MaterialTheme.typography.labelMedium, color = TextDim)
            }

            Spacer(Modifier.height(12.dp))

            // 日期筛选
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = from, onValueChange = { from = it },
                    label = stringResource(R.string.filter_from), modifier = Modifier.weight(1f)
                )
                AppTextField(
                    value = to, onValueChange = { to = it },
                    label = stringResource(R.string.filter_to), modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            if (records.isEmpty()) {
                EmptyHint(stringResource(R.string.list_empty_filter))
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
                                            label = { Text(stringResource(lv.nameRes), style = MaterialTheme.typography.labelSmall) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = lv.color.copy(alpha = 0.25f),
                                                labelColor = lv.color
                                            )
                                        )
                                        if (r.pulse != null) {
                                            Text(stringResource(R.string.bp_pulse, r.pulse), style = MaterialTheme.typography.labelSmall, color = TextDim)
                                        }
                                    }
                                    if (r.note.isNotBlank()) {
                                        Text(r.note, style = MaterialTheme.typography.bodySmall, color = TextDim)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = { onEdit(r.id) }) {
                                        Text(stringResource(R.string.action_edit), color = Gold)
                                    }
                                    var confirm by remember { mutableStateOf(false) }
                                    TextButton(onClick = { confirm = true }) {
                                        Text(stringResource(R.string.action_delete), color = DangerRed)
                                    }
                                    if (confirm) {
                                        ConfirmDelete(
                                            text = stringResource(R.string.bp_delete_confirm, r.date),
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
}

@Composable
fun ConfirmDelete(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_delete_title), color = GoldBright) },
        text = { Text(text, color = TextMain) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete), color = DangerRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextDim) }
        },
        containerColor = NavySoft,
        titleContentColor = GoldBright,
        textContentColor = TextMain
    )
}
