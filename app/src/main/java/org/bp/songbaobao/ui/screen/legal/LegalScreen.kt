package org.bp.songbaobao.ui.screen.legal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.components.SectionTitle
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.PrivacyConsent

/**
 * 合规文档：隐私政策 / 用户协议 / 个人信息与权限清单 / 第三方与开源许可。
 *
 * 这些内容是国内应用商店上架的硬性要求，也是《个人信息保护法》
 * 要求的「告知—同意」义务载体，必须可从应用内直接打开、且便于截图留存。
 *
 * 全部文案放在 strings.xml（values / values-en），
 * 每个条目是一个整串：第一行为标题，后续行是段落，用换行分隔。
 * 这样新增语言只需翻译资源，不必改代码。
 */
@Composable
fun LegalScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(initialTab) }
    val titles = listOf(
        stringResource(R.string.legal_tab_privacy),
        stringResource(R.string.legal_tab_terms),
        stringResource(R.string.legal_tab_compliance)
    )

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.legal_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back), color = Gold)
                }
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
                    stringResource(
                        R.string.legal_updated,
                        PrivacyConsent.CURRENT_VERSION
                    ),
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
    listOf(
        R.string.legal_privacy_1,
        R.string.legal_privacy_2,
        R.string.legal_privacy_3,
        R.string.legal_privacy_4,
        R.string.legal_privacy_5,
        R.string.legal_privacy_6,
        R.string.legal_privacy_7,
        R.string.legal_privacy_8
    ).forEach { res ->
        DocBlockFromString(res)
    }
}

// ---------------------------------------------------------------- 用户协议

@Composable
private fun UserAgreement() {
    listOf(
        R.string.legal_terms_1,
        R.string.legal_terms_2,
        R.string.legal_terms_3,
        R.string.legal_terms_4,
        R.string.legal_terms_5,
        R.string.legal_terms_6,
        R.string.legal_terms_7
    ).forEach { res ->
        DocBlockFromString(res)
    }
}

// ---------------------------------------------------------------- 合规清单

@Composable
private fun ComplianceList() {
    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle(stringResource(R.string.legal_section_info))
            Spacer(Modifier.height(6.dp))
            ListTable(
                headers = stringArrayResource(R.array.legal_info_headers).toList(),
                rows = stringArrayResource(R.array.legal_info_rows)
                    .map { it.split("|") }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.legal_info_note),
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }

    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle(stringResource(R.string.legal_section_perm))
            Spacer(Modifier.height(6.dp))
            ListTable(
                headers = stringArrayResource(R.array.legal_perm_headers).toList(),
                rows = stringArrayResource(R.array.legal_perm_rows)
                    .map { it.split("|") }
            )
        }
    }

    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle(stringResource(R.string.legal_section_sdk))
            Spacer(Modifier.height(6.dp))
            ListTable(
                headers = stringArrayResource(R.array.legal_sdk_headers).toList(),
                rows = stringArrayResource(R.array.legal_sdk_rows)
                    .map { it.split("|") }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.legal_sdk_note),
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }

    PanelCard {
        Column(modifier = Modifier.padding(12.dp)) {
            SectionTitle(stringResource(R.string.legal_section_license))
            Spacer(Modifier.height(6.dp))
            stringArrayResource(R.array.legal_licenses).forEach {
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

/** 将「标题\n段落\n段落」形式的资源渲染成一块文档 */
@Composable
private fun DocBlockFromString(@androidx.annotation.StringRes resId: Int) {
    val lines = stringResource(resId).split("\n")
    PanelCard(modifier = Modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                lines.first(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldBright
            )
            Spacer(Modifier.height(6.dp))
            lines.drop(1).forEach {
                if (it.isBlank()) return@forEach
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

/** 轻量表格：首列较宽，避免引入表格依赖 */
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
                    modifier = Modifier
                        .weight(if (i == 0) 1.4f else 1f)
                        .padding(end = 4.dp)
                )
            }
        }
        HorizontalDivider(color = DividerGold.copy(alpha = 0.18f))
    }
}
