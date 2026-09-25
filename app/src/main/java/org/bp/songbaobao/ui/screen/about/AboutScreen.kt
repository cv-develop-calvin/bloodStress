package org.bp.songbaobao.ui.screen.about

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.CrashHandler
import org.bp.songbaobao.backup.ImportMode
import org.bp.songbaobao.ui.components.AppBackground
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
            "1.4.0" to (
                "2026-09-25" to listOf(
                    "服药提醒按钮与统计文案由「已服」改为「打卡」",
                    "修复药品「购买」按钮跳转报错：改为系统统一处理，装了美团会自动唤起，否则用浏览器打开美团买药搜索页",
                    "补上 Android 11+ 包可见性声明，并避免无应用可处理时崩溃",
                    "关于页的版本号改为随构建自动显示，不再固定写死"
                )
            ),
            "1.3.0" to (
                "2026-09-24" to listOf(
                    "新增数据导出与导入备份，换机或重装可一键恢复",
                    "药品卡片新增「购买」按钮，可直接跳转美团买药",
                    "新增从 GitHub 直接检查、下载并安装新版本",
                    "首页新增标题与射手座、雅典娜主题背景图",
                    "所有功能页面统一使用圣斗士主题背景"
                )
            ),
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

    AppBackground {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HeroCard {
            ZodiacChip("关于 · ABOUT")
            Spacer(Modifier.height(8.dp))
            Text("宋宝宝的记录", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = GoldBright)
            Text(
                "版本 ${org.bp.songbaobao.BuildConfig.VERSION_NAME}" +
                    "（build ${org.bp.songbaobao.BuildConfig.VERSION_CODE}）",
                style = MaterialTheme.typography.bodySmall, color = TextDim
            )
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = {}, label = { Text("正式版") })
        }

        Spacer(Modifier.height(12.dp))

        // 构建信息
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle("构建信息")
                InfoRow("版本号", org.bp.songbaobao.BuildConfig.VERSION_NAME)
                InfoRow("版本代码", "${org.bp.songbaobao.BuildConfig.VERSION_CODE}")
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

        // 系统更新：从 GitHub 检查并安装新版本
        UpdateCard()

        Spacer(Modifier.height(12.dp))

        // 数据备份与恢复
        BackupCard()

        Spacer(Modifier.height(12.dp))

        // 更新日志
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle("更新日志")
                changelog.forEach { (version, pair) ->
                    val (date, items) = pair
                    var expanded by remember {
                        mutableStateOf(version == org.bp.songbaobao.BuildConfig.VERSION_NAME)
                    }
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
}

/**
 * 系统更新卡片：从 GitHub Releases 检查新版本、下载并安装。
 * 全程在应用内完成，无需跳浏览器。
 */
@Composable
private fun UpdateCard(vm: AboutViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val u by vm.update.collectAsState()

    PanelCard {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle("系统更新")
            Text(
                "从 GitHub 检查最新版本，下载后直接安装升级（数据不会丢失）。",
                style = MaterialTheme.typography.bodySmall, color = TextDim
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (u.downloadedApk != null) vm.install(context) else vm.checkUpdate()
                    },
                    enabled = !u.checking && !u.downloading
                ) {
                    when {
                        u.checking -> Text("检查中…")
                        u.downloadedApk != null -> Text("立即安装")
                        else -> Text("检查更新")
                    }
                }

                // 发现新版本后，提供下载入口
                if (u.available != null && u.downloadedApk == null) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { vm.downloadUpdate(context) },
                        enabled = !u.downloading
                    ) { Text(if (u.downloading) "下载中…" else "下载", color = Gold) }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "当前 v${vm.currentVersionName}",
                    style = MaterialTheme.typography.labelSmall, color = TextDim
                )
            }

            // 下载进度
            if (u.downloading) {
                Spacer(Modifier.height(10.dp))
                val p = u.progress
                if (p == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Gold)
                } else {
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.fillMaxWidth(),
                        color = Gold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "已下载 ${(p * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, color = TextDim
                    )
                }
            }

            // 发现新版本：展示版本号、时间、体积与更新说明
            u.available?.let { info ->
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gold.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            "发现新版本 v${info.versionName}（build ${info.versionCode}）",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            buildString {
                                if (info.publishedAt.isNotBlank()) append("发布时间 ${info.publishedAt}")
                                if (info.apkSize > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("体积 ${info.sizeText()}")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall, color = TextDim
                        )
                        if (info.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                info.notes.take(600),
                                style = MaterialTheme.typography.bodySmall, color = TextMain
                            )
                        }
                    }
                }
            }

            // 提示信息
            u.message?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.contains("失败") || msg.contains("无法")) DangerRed else SuccessGreen
                )
                // 下载完成但安装被拦时，给出开启权限的入口
                if (u.downloadedApk != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { vm.openInstallSettings(context) }) {
                        Text("安装被拦截？点此开启安装权限", color = Gold)
                    }
                }
            }

            TextButton(onClick = { vm.dismissUpdate() }) {
                Text("清除更新提示", color = TextDim)
            }
        }
    }
}

/**
 * 数据备份卡片：导出为 JSON 文件、从备份文件恢复。
 * 导出走 SAF 的 CreateDocument（用户自选保存位置），
 * 导入走 OpenDocument（用户自选文件），无需申请存储权限。
 */
@Composable
private fun BackupCard(vm: AboutViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }

    // 导出：先让用户选保存位置
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.export(context, it) } }

    // 导入：先让用户选备份文件，再询问导入模式
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    PanelCard {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle("数据备份")
            Text(
                "把全部记录导出成一个 JSON 文件保存到手机或网盘；" +
                        "换机或重装后可从该文件恢复。",
                style = MaterialTheme.typography.bodySmall, color = TextDim
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { exportLauncher.launch(vm.defaultFileName) },
                    enabled = !state.busy
                ) { Text("导出备份") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    enabled = !state.busy
                ) { Text("从备份恢复", color = Gold) }
            }

            if (state.busy) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Gold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("处理中…", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
            }

            state.message?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (msg.contains("失败") || msg.contains("不是")) DangerRed else SuccessGreen
                )
                state.detail?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
        }
    }

    // 导入模式选择：合并 or 覆盖
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; pendingImportUri = null },
            title = { Text("选择导入方式") },
            text = {
                Column {
                    Text(
                        "合并：保留手机里现有记录，备份中的内容作为新记录追加。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "覆盖：先清空手机里的全部记录，再写入备份内容（不可撤销）。",
                        style = MaterialTheme.typography.bodySmall, color = DangerRed
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { vm.import(context, it, ImportMode.MERGE) }
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text("合并导入", color = SuccessGreen) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { vm.import(context, it, ImportMode.REPLACE) }
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text("覆盖导入", color = DangerRed) }
            }
        )
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
