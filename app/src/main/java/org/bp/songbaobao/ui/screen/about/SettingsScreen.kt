package org.bp.songbaobao.ui.screen.about

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.ui.theme.ThemePresets
import org.bp.songbaobao.util.UserPrefs

/**
 * 个性化设置：系统色系、背景图片、系统标题。
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val schemeId by UserPrefs.themeScheme.collectAsState()
    val bgUri by UserPrefs.bgUri.collectAsState()
    val customTitle by UserPrefs.customTitle.collectAsState()

    var titleInput by remember { mutableStateOf(customTitle ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // 部分机型/来源不支持持久授权，仍按会话内 URI 使用
            }
            UserPrefs.setBgUri(it.toString())
        }
    }

    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text(
                        "‹",
                        color = GoldBright,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldBright
                )
            }
            Spacer(Modifier.height(12.dp))

            // 系统色系
            PanelCard {
                Column(Modifier.padding(12.dp)) {
                    SectionTitle(stringResource(R.string.settings_theme))
                    Text(
                        stringResource(R.string.settings_theme_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        ThemePresets.forEach { preset ->
                            val selected = schemeId == preset.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Gold.copy(alpha = 0.16f) else PanelBg)
                                    .border(
                                        1.dp,
                                        if (selected) Gold else PanelBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { UserPrefs.setThemeScheme(preset.id) }
                                    .padding(10.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(preset.swatch, RoundedCornerShape(50))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(preset.nameRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Gold else TextMain
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 背景图片
            PanelCard {
                Column(Modifier.padding(12.dp)) {
                    SectionTitle(stringResource(R.string.settings_bg))
                    Text(
                        stringResource(R.string.settings_bg_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavySoft)
                    ) {
                        if (bgUri != null) {
                            AsyncImage(
                                model = bgUri,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                stringResource(R.string.settings_bg_default_label),
                                Modifier.align(Alignment.Center),
                                color = TextDim,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        GoldButton(onClick = { pickImage.launch("image/*") }) {
                            Text(stringResource(R.string.settings_bg_pick))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { UserPrefs.setBgUri(null) },
                            enabled = bgUri != null
                        ) {
                            Text(stringResource(R.string.settings_bg_default), color = Gold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 系统标题
            PanelCard {
                Column(Modifier.padding(12.dp)) {
                    SectionTitle(stringResource(R.string.settings_title_custom))
                    Text(
                        stringResource(R.string.settings_title_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it; titleError = false },
                        label = stringResource(R.string.settings_title_custom),
                        placeholder = stringResource(R.string.app_name),
                        isError = titleError,
                        supportingText = if (titleError) stringResource(R.string.settings_title_empty) else null
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        GoldButton(onClick = {
                            val t = titleInput.trim()
                            if (t.isEmpty()) {
                                titleError = true
                                return@GoldButton
                            }
                            UserPrefs.setCustomTitle(t)
                            toast = context.getString(R.string.settings_saved)
                        }) {
                            Text(stringResource(R.string.settings_save))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            UserPrefs.setCustomTitle(null)
                            titleInput = ""
                            toast = context.getString(R.string.settings_reset_done)
                        }) {
                            Text(stringResource(R.string.settings_reset), color = Gold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            toast = null
        }
    }
}
