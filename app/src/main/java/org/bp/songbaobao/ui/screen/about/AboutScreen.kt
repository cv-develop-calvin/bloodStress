package org.bp.songbaobao.ui.screen.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bp.songbaobao.CrashHandler
import org.bp.songbaobao.ui.components.HeroCard
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.components.SectionTitle
import org.bp.songbaobao.ui.components.ZodiacChip
import org.bp.songbaobao.ui.theme.*

/** 关于与版本：构建信息 + 更新日志 */
@Composable
fun AboutScreen() {
    val changelog = remember {
        listOf(
            "1.2.0" to (
                "2026-09-22" to listOf(
                    "使用 Kotlin + Jetpack Compose 原生重构，数据完全保存在手机本地",
                    "血压记录与趋势曲线（7/30/90 天 / 全部）",
                    "用药记录、服药打卡与到点提醒（系统通知 + 手机铃声）",
                    "血常规记录、指标趋势与拍照识别化验单",
                    "笔记本：文字留言 + 照片记录",
                    "星座战士主题：深空星夜背景与金色描边",
                    "全局使用 Noto Sans SC 字体"
                )
            ),
            "1.1.0" to (
                "2026-09-22" to listOf(
                    "新增用药记录与提醒、血常规记录与拍照识别",
                    "拍照识别并入血常规页"
                )
            ),
            "1.0.0" to (
                "2026-09-20" to listOf(
                    "血压记录与趋势曲线",
                    "血压分级判定与健康提示",
                    "记录筛选、编辑与导出"
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HeroCard {
            ZodiacChip("关于 · ABOUT")
            Spacer(Modifier.height(8.dp))
            Text("宋宝宝的记录", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = GoldBright)
            Text("版本 1.2.0（build 10200）",
                style = MaterialTheme.typography.bodySmall, color = TextDim)
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = {}, label = { Text("正式版") })
        }

        Spacer(Modifier.height(12.dp))

        // 构建信息
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle("构建信息")
                InfoRow("版本号", "1.2.0")
                InfoRow("版本代码", "10200")
                InfoRow("最低系统", "Android 8.0（API 26）")
                InfoRow("目标系统", "Android 14（API 34）")
                InfoRow("架构", "arm64-v8a")
                InfoRow("数据库", "Room / SQLite（本地）")
                InfoRow("字体", "Noto Sans SC")
            }
        }

        Spacer(Modifier.height(12.dp))

        // 技术栈
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle("技术栈")
                InfoRow("语言", "Kotlin")
                InfoRow("界面", "Jetpack Compose + Material 3")
                InfoRow("架构", "MVVM（ViewModel + Repository）")
                InfoRow("数据库", "Room")
                InfoRow("依赖注入", "Hilt")
                InfoRow("异步", "Coroutines + Flow")
                InfoRow("导航", "Navigation Compose")
                InfoRow("图表", "MPAndroidChart")
                InfoRow("OCR", "Google ML Kit（中文）")
                InfoRow("图片", "Coil")
            }
        }

        Spacer(Modifier.height(12.dp))

        // 更新日志
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle("更新日志")
                changelog.forEach { (version, pair) ->
                    val (date, items) = pair
                    var expanded by remember { mutableStateOf(version == "1.2.0") }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("v$version", fontWeight = FontWeight.Bold, color = GoldBright)
                                Spacer(Modifier.width(6.dp))
                                Text(date, style = MaterialTheme.typography.labelSmall, color = TextDim)
                            }
                            if (expanded) {
                                Spacer(Modifier.height(4.dp))
                                items.forEach { item ->
                                    Text("· $item", style = MaterialTheme.typography.bodySmall,
                                        color = TextDim, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                                }
                            }
                        }
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "收起" else "展开", color = Gold,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider(color = DividerGold.copy(alpha = 0.35f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 上次崩溃日志（若存在），便于把闪退原因反馈给开发者
        val context = LocalContext.current
        var crash by remember { mutableStateOf(CrashHandler.read(context)) }
        if (crash != null) {
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionTitle("上次崩溃日志")
                    Text(
                        "应用上次运行时异常退出。点击下方按钮把日志发出来即可定位原因。",
                        style = MaterialTheme.typography.bodySmall, color = TextDim
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            context.startActivity(CrashHandler.shareIntent(context, crash!!))
                        }) { Text("分享日志") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            CrashHandler.clear(context)
                            crash = null
                        }) { Text("清除", color = Gold) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        crash!!.take(2000),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            "数据全部保存在手机本地，卸载应用会一并删除，请定期导出备份。",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextDim)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
