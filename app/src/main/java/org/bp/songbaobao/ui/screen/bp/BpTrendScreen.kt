package org.bp.songbaobao.ui.screen.bp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.classifyBp
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.components.BpLineChart
import org.bp.songbaobao.ui.theme.*

@Composable
fun BpTrendScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onList: () -> Unit,
    onScan: () -> Unit = {},
    vm: BpViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = org.bp.songbaobao.ui.theme.Gold,
                contentColor = org.bp.songbaobao.ui.theme.Navy
            ) { Text("＋", fontWeight = FontWeight.Bold) }
        }
    ) { padding ->
        // 射手座 + 雅典娜主题背景（与各功能页共用同一组件）
        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Spacer(Modifier.height(12.dp))

            // 应用标题
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
                            stringResource(R.string.bp_scan_entry_title),
                            fontWeight = FontWeight.Bold, color = GoldBright
                        )
                        Text(
                            stringResource(R.string.bp_scan_entry_sub),
                            style = MaterialTheme.typography.bodySmall, color = TextDim
                        )
                    }
                    Text("›", color = Gold, style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 最新测量（圣衣卡）
            HeroCard {
                ZodiacChip(stringResource(R.string.bp_chip))
                Spacer(Modifier.height(8.dp))
                if (state.latest == null) {
                    Text(stringResource(R.string.bp_no_data),
                        color = org.bp.songbaobao.ui.theme.GoldBright,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    GoldButton(onClick = onAdd) {
                        Text(stringResource(R.string.bp_add_first))
                    }
                } else {
                    val r = state.latest!!
                    val lv = classifyBp(r.systolic, r.diastolic)
                    Text(stringResource(R.string.bp_latest, r.date, r.time),
                        style = MaterialTheme.typography.labelMedium,
                        color = org.bp.songbaobao.ui.theme.TextDim)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${r.systolic}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("/${r.diastolic}",
                            style = MaterialTheme.typography.titleLarge,
                            color = org.bp.songbaobao.ui.theme.TextDim)
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
                        if (r.pulse != null) {
                            AssistChip(onClick = {}, label = {
                                Text(stringResource(R.string.bp_pulse, r.pulse!!))
                            })
                        }
                    }
                    if (r.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(r.note, style = MaterialTheme.typography.bodySmall,
                            color = org.bp.songbaobao.ui.theme.TextDim)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 时间范围
            RangeChips(days = state.days, onSelect = { vm.setDays(it) })

            Spacer(Modifier.height(12.dp))

            // 统计
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.bp_avg_sys),
                    value = state.stats?.avgSys?.let { "%.1f".format(it) } ?: "--",
                    sub = stringResource(
                        R.string.bp_range_max_min,
                        state.stats?.maxSys?.toString() ?: "--",
                        state.stats?.minSys?.toString() ?: "--"
                    ),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.bp_avg_dia),
                    value = state.stats?.avgDia?.let { "%.1f".format(it) } ?: "--",
                    sub = stringResource(
                        R.string.bp_range_max_min,
                        state.stats?.maxDia?.toString() ?: "--",
                        state.stats?.minDia?.toString() ?: "--"
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.bp_avg_pulse),
                    value = state.stats?.avgPulse?.let { "%.1f".format(it) } ?: "--",
                    sub = stringResource(R.string.bp_count_records, state.stats?.n ?: 0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.bp_adherence_7d),
                    value = state.adherence?.rate?.let { "$it%" } ?: "--",
                    sub = stringResource(
                        R.string.med_taken_of,
                        state.adherence?.taken ?: 0,
                        state.adherence?.expected ?: 0
                    ),
                    valueColor = if ((state.adherence?.rate ?: 100) < 80)
                        org.bp.songbaobao.ui.theme.DangerRed
                    else org.bp.songbaobao.ui.theme.SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 待服药提醒
            if (state.pending.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = org.bp.songbaobao.ui.theme.WarnAmber.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏰ ", style = MaterialTheme.typography.titleMedium)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.bp_time_to_take),
                                fontWeight = FontWeight.Bold,
                                color = org.bp.songbaobao.ui.theme.WarnAmber
                            )
                            Text(
                                state.pending.joinToString("、") { "${it.med.name}(${it.slot})" },
                                style = MaterialTheme.typography.bodySmall,
                                color = org.bp.songbaobao.ui.theme.TextDim
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 趋势曲线
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionTitle(stringResource(R.string.bp_trend_title))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToggleChip(stringResource(R.string.bp_series_sys),
                            state.showSys) { vm.toggleSeries(0) }
                        ToggleChip(stringResource(R.string.bp_series_dia),
                            state.showDia) { vm.toggleSeries(1) }
                        ToggleChip(stringResource(R.string.bp_series_pulse),
                            state.showPulse) { vm.toggleSeries(2) }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.trend.isEmpty()) {
                        EmptyHint(
                            stringResource(R.string.bp_no_data),
                            stringResource(R.string.bp_trend_empty_hint)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                            BpLineChart(
                                labels = state.trend.map { it.date.substring(5) },
                                systolic = state.trend.map { it.systolic.toFloat() },
                                diastolic = state.trend.map { it.diastolic.toFloat() },
                                pulse = state.trend.map { it.pulse?.toFloat() },
                                showSys = state.showSys,
                                showDia = state.showDia,
                                showPulse = state.showPulse
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 最近记录
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionTitle(stringResource(R.string.bp_recent_title)) {
                        TextButton(onClick = onList) {
                            Text(
                                stringResource(R.string.bp_view_all),
                                color = org.bp.songbaobao.ui.theme.Gold
                            )
                        }
                    }
                    if (state.recent.isEmpty()) {
                        EmptyHint(stringResource(R.string.bp_no_records))
                    } else {
                        state.recent.forEach { r ->
                            val lv = classifyBp(r.systolic, r.diastolic)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${r.date} ${r.time}", fontWeight = FontWeight.SemiBold)
                                    if (r.note.isNotBlank()) {
                                        Text(r.note, style = MaterialTheme.typography.bodySmall,
                                            color = org.bp.songbaobao.ui.theme.TextDim)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${r.systolic}/${r.diastolic}", fontWeight = FontWeight.Bold)
                                    Text(stringResource(lv.nameRes), style = MaterialTheme.typography.labelSmall, color = lv.color)
                                }
                                IconButton(onClick = { onEdit(r.id) }) {
                                    Text("›", style = MaterialTheme.typography.titleLarge,
                                        color = org.bp.songbaobao.ui.theme.Gold)
                                }
                            }
                            HorizontalDivider(color = org.bp.songbaobao.ui.theme.DividerGold.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
            }
        }
    }
}

/** 首页副标题：压在顶栏自定义标题下方的引导语 */
@Composable
private fun AppHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            stringResource(R.string.bp_app_subtitle),
            style = MaterialTheme.typography.labelSmall,
            color = org.bp.songbaobao.ui.theme.Gold.copy(alpha = 0.75f),
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun RangeChips(days: Int?, onSelect: (Int?) -> Unit) {
    val options: List<Pair<Int?, String>> = listOf(
        7 to stringResource(R.string.bp_range_7d),
        30 to stringResource(R.string.bp_range_30d),
        90 to stringResource(R.string.bp_range_90d),
        null to stringResource(R.string.bp_range_all)
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
                    selectedContainerColor = org.bp.songbaobao.ui.theme.Gold.copy(alpha = 0.25f),
                    selectedLabelColor = org.bp.songbaobao.ui.theme.GoldBright,
                    containerColor = Color.Transparent,
                    labelColor = org.bp.songbaobao.ui.theme.TextDim
                ),
            )
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onToggle: () -> Unit) {
    FilterChip(
        selected = active,
        onClick = onToggle,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = org.bp.songbaobao.ui.theme.Gold.copy(alpha = 0.2f),
            selectedLabelColor = org.bp.songbaobao.ui.theme.GoldBright,
            containerColor = Color.Transparent,
            labelColor = org.bp.songbaobao.ui.theme.TextDim
        )
    )
}
