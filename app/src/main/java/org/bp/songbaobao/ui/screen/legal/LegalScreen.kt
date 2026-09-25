package org.bp.songbaobao.ui.screen.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.components.SectionTitle
import org.bp.songbaobao.ui.theme.*

/**
 * 合规文档：隐私政策 / 用户协议 / 个人信息与权限清单 / 第三方与开源许可。
 *
 * 这些内容是国内应用商店上架的硬性要求，也是《个人信息保护法》
 * 要求的「告知—同意」义务载体，必须可从应用内直接打开、且便于截图留存。
 */
@Composable
fun LegalScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(initialTab) }
    val titles = listOf("隐私政策", "用户协议", "合规清单")

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "隐私与协议",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )
                TextButton(onClick = onBack) { Text("返回", color = Gold) }
            }

            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = GoldBright
            ) {
                titles.forEachIndexed { i, t ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(
                                t,
                                color = if (tab == i) GoldBright else TextDim,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (tab) {
                    0 -> PrivacyPolicy()
                    1 -> UserAgreement()
                    else -> ComplianceList()
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "本文档最后更新：2026-09-25（政策版本 v${org.bp.songbaobao.util.PrivacyConsent.CURRENT_VERSION}）",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- 隐私政策

@Composable
private fun PrivacyPolicy() {
    DocBlock(
        title = "一、我们收集哪些信息",
        lines = listOf(
            "本应用是一款本地优先的健康记录工具。你录入的血压、用药、血常规、笔记与照片，" +
                "默认全部保存在手机本地数据库中，不会自动上传到任何服务器。",
            "需要特别说明：健康医疗数据属于《个人信息保护法》所称的敏感个人信息。" +
                "我们仅在为你提供记录、提醒、趋势统计与你主动发起的导出功能时使用这些数据，" +
                "不会用于广告、画像或任何无关用途。"
        )
    )
    DocBlock(
        title = "二、网络访问的用途",
        lines = listOf(
            "应用仅在以下三种由你主动触发的场景访问网络：",
            "1. 检查更新：请求 GitHub（api.github.com）获取最新版本号，不携带任何个人数据；",
            "2. 下载安装：从 GitHub 下载 APK 安装包；",
            "3. 药品购买：跳转美团买药搜索页（i.meituan.com），跳转时仅传递药品名称用于搜索。",
            "除此之外，应用不会在后台静默联网，也不存在任何隐式的用户行为上报。"
        )
    )
    DocBlock(
        title = "三、崩溃日志",
        lines = listOf(
            "应用发生闪退时，会把崩溃堆栈写入手机本地文件（应用私有目录 crash/last_crash.txt），" +
                "内容包含时间、设备型号、系统版本、应用版本与异常堆栈。",
            "该日志不会自动上传。只有你在「版本」页主动点击「分享日志」时才会发出，" +
                "发出前你可以看到完整内容。"
        )
    )
    DocBlock(
        title = "四、系统权限说明",
        lines = listOf(
            "通知权限：用于到点推送服药提醒；",
            "精确闹钟：用于在设定的服药时间准时触发提醒；",
            "开机启动：用于手机重启后重新注册提醒，不会因此收集数据；",
            "网络访问：仅用于上述检查更新、下载安装包与药品购买跳转；",
            "安装未知应用：用于安装你主动下载的新版本；",
            "相册读取：仅在识别化验单、为笔记添加照片时由你选择图片后使用。",
            "所有权限都在用到时才申请，你可以随时在系统设置中关闭，" +
                "关闭后相应功能不可用，但不影响其余功能。"
        )
    )
    DocBlock(
        title = "五、数据存储与安全",
        lines = listOf(
            "数据保存在手机本地数据库（SQLite/Room）与应用私有目录中，" +
                "其他应用无法直接读取。",
            "重要提醒：卸载应用会一并删除全部本地数据。请定期使用「数据备份」导出 JSON 文件，" +
                "保存到网盘或电脑，以便换机、重装或误删后恢复。"
        )
    )
    DocBlock(
        title = "六、你的权利",
        lines = listOf(
            "你可以随时查看、修改、删除自己录入的记录；",
            "可以通过「数据备份」导出全部数据；",
            "可以删除某条记录或清空全部数据；",
            "可以撤回对隐私政策的同意（撤回后应用将停止收集并退出）。"
        )
    )
    DocBlock(
        title = "七、未成年人信息",
        lines = listOf(
            "本应用面向一般成年人及其照护对象使用。若由监护人为被照护人录入健康数据，" +
                "相关数据的处理方式与本政策一致，同样仅保存在本地。"
        )
    )
    DocBlock(
        title = "八、政策更新",
        lines = listOf(
            "本政策如有实质性修改，我们会在应用内更新文档并重新征得你的同意。",
            "继续使用即表示你接受更新后的政策。"
        )
    )
}

// ---------------------------------------------------------------- 用户协议

@Composable
private fun UserAgreement() {
    DocBlock(
        title = "一、服务内容",
        lines = listOf(
            "本应用为你提供血压记录与趋势、用药提醒与打卡、血常规记录与拍照识别、" +
                "笔记留言、数据备份与恢复，以及应用内版本升级等工具功能。"
        )
    )
    DocBlock(
        title = "二、重要免责声明",
        lines = listOf(
            "本应用是健康记录工具，不是医疗器械，不提供诊断、治疗建议或医嘱。",
            "应用中展示的血压分级、指标参考范围与提示均来自公开资料，仅供参考，" +
                "不能替代执业医师的判断。",
            "任何用药、停药、剂量调整都必须遵医嘱。因自行判断造成的后果由用户自行承担。",
            "如出现身体不适，请及时就医，不要依据本应用的统计结果自行处理。"
        )
    )
    DocBlock(
        title = "三、数据责任",
        lines = listOf(
            "你的数据保存在本机，你对自己数据的安全与备份负责。",
            "我们建议你定期导出备份。因设备丢失、损坏、卸载应用或系统清理导致的数据丢失，" +
                "我们无法代为恢复。"
        )
    )
    DocBlock(
        title = "四、账号与付费",
        lines = listOf(
            "当前版本不提供账号体系，无需注册登录，也不收取任何费用。"
        ),
    )
    DocBlock(
        title = "五、知识产权",
        lines = listOf(
            "本应用的界面设计、文案与代码归开发者所有。",
            "应用内使用的第三方开源组件遵循各自的开源许可证，详见「合规清单」页。"
        )
    )
    DocBlock(
        title = "六、服务变更与终止",
        lines = listOf(
            "我们可能出于功能调整、合规要求或技术原因变更、暂停部分功能，" +
                "并将尽力在应用内告知。",
            "若你违反法律法规或本协议使用本应用，我们有权终止向您提供服务。"
        )
    )
    DocBlock(
        title = "七、争议解决",
        lines = listOf(
            "本协议适用中华人民共和国法律。因本协议产生的争议应友好协商解决；" +
                "协商不成的，提交开发者所在地有管辖权的人民法院处理。"
        )
    )
}

// ---------------------------------------------------------------- 合规清单

@Composable
private fun ComplianceList() {
    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle("个人信息收集清单")
            Spacer(Modifier.height(6.dp))
            ListTable(
                headers = listOf("信息类型", "用途", "是否上传"),
                rows = listOf(
                    listOf("血压记录", "记录与趋势统计", "仅本地"),
                    listOf("用药与服药打卡", "提醒与依从性统计", "仅本地"),
                    listOf("血常规指标", "记录与趋势", "仅本地"),
                    listOf("笔记文字与照片", "留言记录", "仅本地"),
                    listOf("崩溃日志", "定位闪退原因", "仅本地，手动分享才发出"),
                    listOf("隐私政策同意状态", "记录同意事实", "仅本地")
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "上述信息中，健康医疗数据属于敏感个人信息，仅在本地处理，不会上传。",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }

    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle("系统权限清单")
            Spacer(Modifier.height(6.dp))
            ListTable(
                headers = listOf("权限", "用途"),
                rows = listOf(
                    listOf("POST_NOTIFICATIONS", "服药提醒通知"),
                    listOf("SCHEDULE_EXACT_ALARM", "到点准时提醒"),
                    listOf("VIBRATE", "提醒震动"),
                    listOf("RECEIVE_BOOT_COMPLETED", "重启后恢复提醒"),
                    listOf("INTERNET", "检查更新、下载安装包、购药跳转"),
                    listOf("ACCESS_NETWORK_STATE", "判断网络可用性"),
                    listOf("REQUEST_INSTALL_PACKAGES", "安装下载的新版本"),
                    listOf("READ_MEDIA_IMAGES", "识别化验单、添加笔记照片")
                )
            )
        }
    }

    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle("第三方 SDK 清单")
            Spacer(Modifier.height(6.dp))
            ListTable(
                headers = listOf("SDK", "用途", "数据"),
                rows = listOf(
                    listOf("Google ML Kit 文字识别（中文）", "化验单拍照识别", "设备端处理，不上传"),
                    listOf("Coil", "图片加载", "不收集"),
                    listOf("MPAndroidChart", "趋势图表", "不收集"),
                    listOf("AndroidX Room", "本地数据库", "不收集"),
                    listOf("AndroidX / Compose", "界面与导航", "不收集"),
                    listOf("Hilt", "依赖注入", "不收集")
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "本应用未接入任何广告、推送统计或用户行为分析 SDK。",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }

    PanelCard {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle("开源许可声明")
            Spacer(Modifier.height(6.dp))
            listOf(
                "AndroidX 系列组件（Apache License 2.0）",
                "Jetpack Compose（Apache License 2.0）",
                "Kotlin 标准库与协程（Apache License 2.0）",
                "Hilt / Dagger（Apache License 2.0）",
                "Coil（Apache License 2.0）",
                "MPAndroidChart（Apache License 2.0）",
                "Google ML Kit（Google 服务条款）",
                "Material Design 图标（Apache License 2.0）"
            ).forEach {
                Text(
                    "· $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 通用组件

@Composable
private fun DocBlock(title: String, lines: List<String>) {
    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldBright
            )
            Spacer(Modifier.height(6.dp))
            lines.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMain,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/** 轻量表格：首列为表头，用行内排版避免引入表格依赖 */
@Composable
private fun ListTable(headers: List<String>, rows: List<List<String>>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        headers.forEachIndexed { i, h ->
            Text(
                h,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Gold,
                modifier = Modifier.weight(if (i == 0) 1.4f else 1f)
            )
        }
    }
    HorizontalDivider(color = DividerGold.copy(alpha = 0.35f))
    rows.forEach { row ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            row.forEachIndexed { i, cell ->
                Text(
                    cell,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMain,
                    modifier = Modifier.weight(if (i == 0) 1.4f else 1f)
                        .padding(end = 4.dp)
                )
            }
        }
        HorizontalDivider(color = DividerGold.copy(alpha = 0.18f))
    }
}
