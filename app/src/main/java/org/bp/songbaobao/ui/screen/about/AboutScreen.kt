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
import androidx.compose.ui.res.stringResource
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.ZodiacChip
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.LanguageManager

/** 关于与版本：构建信息 + 更新日志 */
@Composable
fun AboutScreen(onLegal: () -> Unit = {}) {
    val changelog = remember {
        listOf(
            "1.7.0" to (
                "2026-09-26" to listOf(
                    "新增中英双语切换：关于页-语言，可选跟随系统 / 中文 / English",
                    "全部页面（血压、用药、化验、笔记、关于、合规）文案支持英文",
                    "血压分级提示、化验单偏高/偏低判定、图表标签同步翻译"
                )
            ),
            "1.6.1" to (
                "2026-09-26" to listOf(
                    "修复「购买」按钮跳转报错：原美团买药链接已失效（404），改用有效地址",
                    "购药改为可选渠道：美团买药 / 京东健康 / 淘宝 / 阿里健康",
                    "新增「复制药名」兜底：渠道打不开时可粘贴到常用购药 App 搜索",
                    "打开前预检可处理的应用，避免渠道不可用时闪退"
                )
            ),
            "1.6.0" to (
                "2026-09-25" to listOf(
                    "启用正式发布签名（密钥不再入库，改由 GitHub Secrets 注入）",
                    "新增隐私政策、用户协议与合规清单页（关于页可进入）",
                    "首次启动需阅读并同意隐私政策后方可使用",
                    "新增个人信息收集清单、系统权限清单、第三方 SDK 与开源许可声明",
                    "注意：签名更换为正式密钥，从更早版本升级时需先卸载旧版（请先导出备份）"
                )
            ),
            "1.5.0" to (
                "2026-09-25" to listOf(
                    "统一 APK 签名：本机构建与 GitHub 构建改用同一份签名密钥",
                    "从本版起升级不再需要先卸载旧版本，直接覆盖安装即可，数据不会丢失",
                    "注意：从 v1.4.0 及更早版本首次升级时，因签名变更仍需卸载一次（请先导出备份）"
                )
            ),
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
            ZodiacChip(stringResource(R.string.about_chip))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = GoldBright)
            Text(
                stringResource(
                    R.string.about_version,
                    org.bp.songbaobao.BuildConfig.VERSION_NAME,
                    org.bp.songbaobao.BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.bodySmall, color = TextDim
            )
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.about_channel)) })
        }

        Spacer(Modifier.height(12.dp))

        // 语言切换
        LanguageCard()

        Spacer(Modifier.height(12.dp))

        // 构建信息
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle(stringResource(R.string.about_build_info))
                InfoRow(
                    stringResource(R.string.about_version_name),
                    org.bp.songbaobao.BuildConfig.VERSION_NAME
                )
                InfoRow(
                    stringResource(R.string.about_version_code),
                    "${org.bp.songbaobao.BuildConfig.VERSION_CODE}"
                )
                InfoRow(
                    stringResource(R.string.about_min_system),
                    stringResource(R.string.about_value_min_system)
                )
                InfoRow(
                    stringResource(R.string.about_target_system),
                    stringResource(R.string.about_value_target_system)
                )
                InfoRow(
                    stringResource(R.string.about_arch),
                    stringResource(R.string.about_value_arch)
                )
                InfoRow(
                    stringResource(R.string.about_database),
                    stringResource(R.string.about_value_database)
                )
                InfoRow(
                    stringResource(R.string.about_font),
                    stringResource(R.string.about_value_font)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 技术栈
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle(stringResource(R.string.about_tech_stack))
                InfoRow(stringResource(R.string.about_tech_lang),
                    stringResource(R.string.about_value_lang))
                InfoRow(stringResource(R.string.about_tech_ui),
                    stringResource(R.string.about_value_ui))
                InfoRow(stringResource(R.string.about_tech_arch),
                    stringResource(R.string.about_value_arch_mvvm))
                InfoRow(stringResource(R.string.about_database),
                    stringResource(R.string.about_value_db))
                InfoRow(stringResource(R.string.about_tech_di),
                    stringResource(R.string.about_value_di))
                InfoRow(stringResource(R.string.about_tech_async),
                    stringResource(R.string.about_value_async))
                InfoRow(stringResource(R.string.about_tech_nav),
                    stringResource(R.string.about_value_nav))
                InfoRow(stringResource(R.string.about_tech_chart),
                    stringResource(R.string.about_value_chart))
                InfoRow(stringResource(R.string.about_tech_ocr),
                    stringResource(R.string.about_value_ocr))
                InfoRow(stringResource(R.string.about_tech_image),
                    stringResource(R.string.about_value_image))
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
                SectionTitle(stringResource(R.string.about_changelog))
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
                            Text(if (expanded) stringResource(R.string.about_collapse) else stringResource(R.string.about_expand), color = Gold,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider(color = DividerGold.copy(alpha = 0.35f))
                }
            }
        }

        // 合规文档入口：隐私政策、用户协议、个人信息与权限清单、第三方与开源许可
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                SectionTitle(stringResource(R.string.about_privacy_compliance))
                TextButton(onClick = onLegal) {
                    Text(stringResource(R.string.about_view_legal), color = Gold)
                }
                val acceptedAt = org.bp.songbaobao.util.PrivacyConsent
                    .acceptedAt(androidx.compose.ui.platform.LocalContext.current)
                Text(
                    if (acceptedAt != null) stringResource(R.string.about_consent_agreed, acceptedAt) else stringResource(R.string.about_consent_not),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.about_consent_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 上次崩溃日志（若存在），便于把闪退原因反馈给开发者
        val context = LocalContext.current
        var crash by remember { mutableStateOf(CrashHandler.read(context)) }
        if (crash != null) {
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionTitle(stringResource(R.string.about_last_crash))
                    Text(
                        stringResource(R.string.about_crash_desc),
                        style = MaterialTheme.typography.bodySmall, color = TextDim
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            context.startActivity(CrashHandler.shareIntent(context, crash!!))
                        }) { Text(stringResource(R.string.about_share_log)) }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            CrashHandler.clear(context)
                            crash = null
                        }) { Text(stringResource(R.string.about_clear), color = Gold) }
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
            stringResource(R.string.about_data_warning),
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )

        Spacer(Modifier.height(24.dp))
    }
    }
}

/**
 * 从 Context 递归解包出 Activity。
 * Compose 的 LocalContext 通常是 ContextThemeWrapper 等包装类，
 * 直接 as? Activity 会失败，导致 recreate 调不到、语言切换看起来「没生效」。
 */
private fun findActivity(context: android.content.Context): android.app.Activity? {
    var ctx: android.content.Context? = context
    while (ctx != null) {
        if (ctx is android.app.Activity) return ctx
        ctx = (ctx as? android.content.ContextWrapper)?.baseContext
    }
    return null
}

/**
 * 语言切换卡片。
 *
 * 三态：跟随系统 / 中文 / English。
 * 选中后写入偏好并重建 Activity —— 语言是在 attachBaseContext 注入的，
 * 只有重建才能重新走一遍资源解析，让整个界面立即换成新语言。
 */
@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    val options = listOf(
        LanguageManager.FOLLOW_SYSTEM to R.string.lang_follow_system,
        LanguageManager.ZH to R.string.lang_chinese,
        LanguageManager.EN to R.string.lang_english
    )
    var selected by remember { mutableStateOf(LanguageManager.getSavedLanguage(context)) }

    PanelCard {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle(stringResource(R.string.lang_title))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (code, res) ->
                    FilterChip(
                        selected = selected == code,
                        onClick = {
                            if (selected == code) return@FilterChip
                            selected = code
                            LanguageManager.setLanguage(context, code)
                            // 语言在 attachBaseContext 注入，只有重建 Activity
                            // 才会重新走资源解析，因此这里必须触发重建。
                            // LocalContext 通常是 ContextWrapper，需递归解包找 Activity。
                            val activity = findActivity(context)
                            if (activity != null) {
                                activity.recreate()
                            } else {
                                context.startActivity(
                                    android.content.Intent(
                                        context,
                                        org.bp.songbaobao.MainActivity::class.java
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                )
                            }
                        },
                        label = { Text(stringResource(res)) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.lang_restart_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
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
            SectionTitle(stringResource(R.string.about_update))
            Text(
                stringResource(R.string.about_update_desc),
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
                        u.checking -> Text(stringResource(R.string.update_checking))
                        u.downloadedApk != null -> Text(stringResource(R.string.update_install_now))
                        else -> Text(stringResource(R.string.update_check_now))
                    }
                }

                // 发现新版本后，提供下载入口
                if (u.available != null && u.downloadedApk == null) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { vm.downloadUpdate(context) },
                        enabled = !u.downloading
                    ) { Text(if (u.downloading) stringResource(R.string.update_downloading) else stringResource(R.string.update_download), color = Gold) }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.about_current_version, vm.currentVersionName),
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
                        stringResource(R.string.update_downloaded_pct, (p * 100).toInt()),
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
                            stringResource(R.string.update_found, info.versionName, info.versionCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            buildString {
                                if (info.publishedAt.isNotBlank()) append(stringResource(R.string.update_published, info.publishedAt))
                                if (info.apkSize > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append(stringResource(R.string.update_size, info.sizeText()))
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
                        Text(stringResource(R.string.update_install_blocked), color = Gold)
                    }
                }
            }

            TextButton(onClick = { vm.dismissUpdate() }) {
                Text(stringResource(R.string.update_clear_hint), color = TextDim)
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
            SectionTitle(stringResource(R.string.about_backup))
            Text(
                stringResource(R.string.backup_desc),
                style = MaterialTheme.typography.bodySmall, color = TextDim
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { exportLauncher.launch(vm.defaultFileName) },
                    enabled = !state.busy
                ) { Text(stringResource(R.string.backup_export)) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    enabled = !state.busy
                ) { Text(stringResource(R.string.backup_import), color = Gold) }
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
                    Text(stringResource(R.string.common_processing), style = MaterialTheme.typography.bodySmall, color = TextDim)
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
            title = { Text(stringResource(R.string.import_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.import_merge_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.import_replace_desc),
                        style = MaterialTheme.typography.bodySmall, color = DangerRed
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { vm.import(context, it, ImportMode.MERGE) }
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text(stringResource(R.string.import_merge), color = SuccessGreen) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { vm.import(context, it, ImportMode.REPLACE) }
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text(stringResource(R.string.import_replace), color = DangerRed) }
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
