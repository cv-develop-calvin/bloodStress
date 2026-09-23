package org.bp.songbaobao.ui.screen.bp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.classifyBp
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.components.BpLineChart

@Composable
fun BpTrendScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onList: () -> Unit,
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

            // 最新测量（圣衣卡）
            HeroCard {
                ZodiacChip("天马座 · PEGASUS")
                Spacer(Modifier.height(8.dp))
                if (state.latest == null) {
                    Text("暂无数据", color = org.bp.songbaobao.ui.theme.GoldBright,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    GoldButton(onClick = onAdd) { Text("添加第一条记录") }
                } else {
                    val r = state.latest!!
                    val lv = classifyBp(r.systolic, r.diastolic)
                    Text("最新测量 · ${r.date} ${r.time}",
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
                            label = { Text(lv.name) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = lv.color.copy(alpha = 0.25f),
                                labelColor = lv.color
                            )
                        )
                        if (r.pulse != null) {
                            AssistChip(onClick = {}, label = { Text("心率 ${r.pulse}") })
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
                    label = "平均收缩压",
                    value = state.stats?.avgSys?.let { "%.1f".format(it) } ?: "--",
                    sub = "最高 ${state.stats?.maxSys ?: "--"} · 最低 ${state.stats?.minSys ?: "--"}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "平均舒张压",
                    value = state.stats?.avgDia?.let { "%.1f".format(it) } ?: "--",
                    sub = "最高 ${state.stats?.maxDia ?: "--"} · 最低 ${state.stats?.minDia ?: "--"}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "平均心率",
                    value = state.stats?.avgPulse?.let { "%.1f".format(it) } ?: "--",
                    sub = "记录 ${state.stats?.n ?: 0} 条",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "近 7 天服药",
                    value = state.adherence?.rate?.let { "$it%" } ?: "--",
                    sub = "已服 ${state.adherence?.taken ?: 0} / ${state.adherence?.expected ?: 0}",
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
                            Text("该吃药了", fontWeight = FontWeight.Bold, color = org.bp.songbaobao.ui.theme.WarnAmber)
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
                    SectionTitle("血压趋势曲线")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToggleChip("收缩压", state.showSys) { vm.toggleSeries(0) }
                        ToggleChip("舒张压", state.showDia) { vm.toggleSeries(1) }
                        ToggleChip("心率", state.showPulse) { vm.toggleSeries(2) }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.trend.isEmpty()) {
                        EmptyHint("暂无数据", "点击右下角＋添加记录")
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
                    SectionTitle("最近记录") {
                        TextButton(onClick = onList) { Text("查看全部 →", color = org.bp.songbaobao.ui.theme.Gold) }
                    }
                    if (state.recent.isEmpty()) {
                        EmptyHint("还没有记录")
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
                                    Text(lv.name, style = MaterialTheme.typography.labelSmall, color = lv.color)
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

/** 首页大标题：宋宝宝的记录 */
@Composable
private fun AppHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "宋宝宝的记录",
            // 柔和的深色阴影，保证压在亮背景上依然清晰
            style = MaterialTheme.typography.headlineMedium.copy(
                shadow = Shadow(
                    color = org.bp.songbaobao.ui.theme.Navy,
                    offset = Offset(0f, 2f),
                    blurRadius = 12f
                )
            ),
            fontWeight = FontWeight.Bold,
            color = org.bp.songbaobao.ui.theme.GoldBright
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "SAINT · 健康档案",
            style = MaterialTheme.typography.labelSmall,
            color = org.bp.songbaobao.ui.theme.Gold.copy(alpha = 0.75f),
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun RangeChips(days: Int?, onSelect: (Int?) -> Unit) {
    val options = listOf(7 to "7 天", 30 to "30 天", 90 to "90 天", null to "全部")
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
