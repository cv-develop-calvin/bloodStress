package org.bp.songbaobao.ui.screen.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.ui.components.CbcJudge
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.components.LabLineChart
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.data.repository.LabPoint

@Composable
fun LabScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onScan: () -> Unit,
    vm: LabViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    var showScan by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = Gold, contentColor = Navy) {
                Text("＋", fontWeight = FontWeight.Bold)
            }
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
            // 拍照识别（内嵌折叠面板，与 Web 版一致）
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.lab_scan_title),
                                fontWeight = FontWeight.Bold, color = GoldBright
                            )
                            Text(
                                stringResource(R.string.lab_scan_tip),
                                style = MaterialTheme.typography.labelSmall, color = TextDim
                            )
                        }
                        TextButton(onClick = { showScan = !showScan }) {
                            Text(
                                stringResource(
                                    if (showScan) R.string.about_collapse else R.string.about_expand
                                ),
                                color = Gold
                            )
                        }
                    }
                    if (showScan) {
                        Spacer(Modifier.height(8.dp))
                        GoldButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.lab_open_camera))
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onScan,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
                        ) { Text(stringResource(R.string.lab_from_gallery)) }
                        Spacer(Modifier.height(6.dp))
                        val supported = StringBuilder()
                        for (item in CbcItems.items()) {
                            if (supported.isNotEmpty()) supported.append("、")
                            supported.append(stringResource(item.labelRes))
                        }
                        Text(
                            stringResource(R.string.lab_supported, supported.toString()),
                            style = MaterialTheme.typography.labelSmall, color = TextDim
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.reports.isEmpty()) {
                EmptyHint(
                    stringResource(R.string.lab_empty_title),
                    stringResource(R.string.lab_empty_hint)
                )
            }

            // 最新报告
            state.latest?.let { r ->
                PanelCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionTitle(stringResource(R.string.lab_latest, r.date)) {
                            if (r.hospital.isNotBlank()) {
                                Text(r.hospital, style = MaterialTheme.typography.labelSmall, color = TextDim)
                            }
                        }
                        val cols = 2
                        // 用自定义名字避免与标准库 Iterable.chunked 产生解析歧义
                        CbcItems.items().chunkedSafe(cols).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { item ->
                                    val v = r.valueOf(item.key)
                                    val judge = CbcJudge.of(v, item)
                                    Box(modifier = Modifier.weight(1f)) {
                                        Column(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(stringResource(item.labelRes), style = MaterialTheme.typography.labelSmall, color = TextDim)
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text(
                                                    v?.let { fmt(it) } ?: "—",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text(item.unit, style = MaterialTheme.typography.labelSmall, color = TextDim)
                                            }
                                            Text(
                                                "${stringResource(judge.nameRes)} · ${fmt(item.low)}–${fmt(item.high)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = judge.color
                                            )
                                        }
                                    }
                                }
                                if (row.size < cols) {
                                    Spacer(Modifier.weight((cols - row.size).toFloat()))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 指标趋势
            if (state.reports.isNotEmpty()) {
                PanelCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionTitle(stringResource(R.string.lab_trend_title))

                        // 指标选择
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CbcItems.items().forEach { item ->
                                FilterChip(
                                    selected = state.seriesItem == item.key,
                                    onClick = { vm.setSeriesItem(item.key) },
                                    label = { Text(stringResource(item.labelRes), style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold.copy(alpha = 0.25f),
                                        selectedLabelColor = GoldBright,
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        labelColor = TextDim
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 时间范围
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                7 to stringResource(R.string.lab_range_7d),
                                30 to stringResource(R.string.lab_range_30d),
                                90 to stringResource(R.string.lab_range_90d),
                                null to stringResource(R.string.bp_range_all)
                            ).forEach { (v, label) ->
                                FilterChip(
                                    selected = state.days == v,
                                    onClick = { vm.setDays(v) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold.copy(alpha = 0.2f),
                                        selectedLabelColor = GoldBright,
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        labelColor = TextDim
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val item = vm.item(state.seriesItem)
                        if (state.series.isEmpty()) {
                            EmptyHint(stringResource(R.string.lab_no_series))
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                LabLineChart(
                                    labels = state.series.map { it.date.substring(5) },
                                    values = state.series.map { it.value.toFloat() },
                                    low = item.low.toFloat(),
                                    high = item.high.toFloat()
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 历史列表
            state.reports.forEach { r ->
                PanelCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${r.date} ${r.time}", fontWeight = FontWeight.SemiBold)
                                val abnormal = CbcItems.abnormalCount(r.values())
                                Text(
                                    buildString {
                                        if (r.hospital.isNotBlank()) append("${r.hospital} · ")
                                        append(
                                            if (abnormal > 0) {
                                                stringResource(
                                                    R.string.lab_abnormal_count, abnormal
                                                )
                                            } else {
                                                stringResource(R.string.lab_all_normal)
                                            }
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (abnormal > 0) WarnAmber else SuccessGreen
                                )
                            }
                            if (r.source == "photo") {
                                AssistChip(onClick = {}, label = {
                                    Text(stringResource(R.string.lab_source_photo))
                                })
                            }
                            TextButton(onClick = { onEdit(r.id) }) {
                                Text(stringResource(R.string.action_edit), color = Gold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
      }
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

private fun <T> List<T>.chunkedSafe(size: Int): List<List<T>> {
    val out = mutableListOf<List<T>>()
    var i = 0
    while (i < this.size) {
        out.add(subList(i, minOf(i + size, this.size)))
        i += size
    }
    return out
}
