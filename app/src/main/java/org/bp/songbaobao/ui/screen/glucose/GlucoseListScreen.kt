package org.bp.songbaobao.ui.screen.glucose

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.theme.*

/** 血糖首页：最新记录、统计、趋势、最近记录（与血压首页 BpTrendScreen 同结构）。 */
@Composable
fun GlucoseListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onList: () -> Unit,
    onScan: () -> Unit = {},
    vm: GlucoseViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Text("＋", fontWeight = FontWeight.Bold) }
        }
    ) { padding ->
        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                AppHeader()
                Spacer(Modifier.height(12.dp))

                // 拍照识别录入入口
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onScan() },
                    colors = CardDefaults.cardColors(containerColor = ChartPulse.copy(alpha = 0.14f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📷", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.glucose_scan_entry_title),
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.glucose_scan_entry_sub),
                                style = MaterialTheme.typography.bodySmall, color = TextDim
                            )
                        }
                        Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 最新测量
                HeroCard {
                    ZodiacChip(stringResource(R.string.glucose_chip))
                    Spacer(Modifier.height(8.dp))
                    if (state.latest == null) {
                        Text(
                            stringResource(R.string.glucose_no_data),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        GoldButton(onClick = onAdd) {
                            Text(stringResource(R.string.glucose_add_first))
                        }
                    } else {
                        val r = state.latest!!
                        val ctx = GlucoseContext.fromKey(r.context)
                        val lv = classifyGlucose(r.value, ctx)
                        Text(
                            stringResource(R.string.glucose_latest, r.date, r.time),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextDim
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.1f".format(r.value),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                " mmol/L",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextDim
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(stringResource(lv.nameRes)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = lv.color.copy(alpha = 0.25f),
                                    labelColor = lv.color
                                )
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(stringResource(ctx.labelRes)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = TextDim
                                )
                            )
                        }
                        if (r.note.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(r.note, style = MaterialTheme.typography.bodySmall, color = TextDim)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                RangeChips(days = state.days, onSelect = { vm.setDays(it) })
                Spacer(Modifier.height(12.dp))

                // 统计
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.glucose_avg),
                        value = state.stats?.avgValue?.let { "%.1f".format(it) } ?: "--",
                        sub = stringResource(
                            R.string.glucose_range_max_min,
                            state.stats?.maxValue?.let { "%.1f".format(it) } ?: "--",
                            state.stats?.minValue?.let { "%.1f".format(it) } ?: "--"
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.glucose_min),
                        value = state.stats?.minValue?.let { "%.1f".format(it) } ?: "--",
                        sub = stringResource(
                            R.string.glucose_max,
                            state.stats?.maxValue?.let { "%.1f".format(it) } ?: "--"
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.glucose_count),
                        value = "${state.stats?.n ?: 0}",
                        sub = stringResource(R.string.glucose_unit_mmol),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.glucose_recent_title),
                        value = "${state.recent.size}",
                        sub = stringResource(R.string.glucose_recent_sub),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 趋势曲线
                PanelCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionTitle(stringResource(R.string.glucose_trend_title))
                        Spacer(Modifier.height(8.dp))
                        if (state.trend.isEmpty()) {
                            EmptyHint(
                                stringResource(R.string.glucose_no_data),
                                stringResource(R.string.glucose_trend_empty_hint)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                                GlucoseLineChart(
                                    labels = state.trend.map { it.date.substring(5) },
                                    values = state.trend.map { it.value }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 最近记录
                PanelCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionTitle(stringResource(R.string.glucose_recent_title)) {
                            TextButton(onClick = onList) {
                                Text(
                                    stringResource(R.string.glucose_view_all),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (state.recent.isEmpty()) {
                            EmptyHint(stringResource(R.string.glucose_no_records))
                        } else {
                            state.recent.forEach { r ->
                                val ctx = GlucoseContext.fromKey(r.context)
                                val lv = classifyGlucose(r.value, ctx)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${r.date} ${r.time}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            stringResource(ctx.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextDim
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("%.1f".format(r.value), fontWeight = FontWeight.Bold)
                                        Text(
                                            stringResource(lv.nameRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = lv.color
                                        )
                                    }
                                    IconButton(onClick = { onEdit(r.id) }) {
                                        Text(
                                            "›",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

/** 首页副标题 */
@Composable
private fun AppHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            stringResource(R.string.glucose_app_subtitle),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun RangeChips(days: Int?, onSelect: (Int?) -> Unit) {
    val options: List<Pair<Int?, String>> = listOf(
        7 to stringResource(R.string.glucose_range_7d),
        30 to stringResource(R.string.glucose_range_30d),
        90 to stringResource(R.string.glucose_range_90d),
        null to stringResource(R.string.glucose_range_all)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = days == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.Transparent,
                    labelColor = TextDim
                ),
            )
        }
    }
}
