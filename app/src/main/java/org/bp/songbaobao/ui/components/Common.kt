package org.bp.songbaobao.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.UserPrefs

/** 主按钮（强调色随「系统色系」动态变化，默认金色的圣衣色） */
@Composable
fun GoldButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            disabledContainerColor = scheme.primary.copy(alpha = 0.4f),
            disabledContentColor = scheme.onPrimary.copy(alpha = 0.6f)
        ),
        content = content
    )
}

/** 通用面板卡片（毛玻璃近似 + 金色描边） */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorder),
        content = content
    )
}

/** 首屏「圣衣卡」：金色渐变强调 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(NavySoft.copy(alpha = 0.95f), Ink.copy(alpha = 0.98f))
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

/** 统计小卡 */
@Composable
fun StatCard(
    label: String,
    value: String,
    sub: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    PanelCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextDim)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextDim)
        }
    }
}

/** 星座角标 */
@Composable
fun ZodiacChip(text: String) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            letterSpacing = 2.sp
        )
    }
}

/** 空态提示 */
@Composable
fun EmptyHint(text: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = TextDim, textAlign = TextAlign.Center)
        if (sub != null) {
            Spacer(Modifier.height(4.dp))
            Text(sub, color = TextDim, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

/** 带标签的分区标题 */
@Composable
fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

/** 通用文本输入。
 * @param clearOnFocus 为 true 时，每次聚焦（点击）该栏位都会清空其内容，
 *   方便直接重新输入（含默认 0 也会清空）。 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    clearOnFocus: Boolean = false
) {
    // 用 InteractionSource 观察聚焦，并在聚焦完成后的协程里清空，
    // 避免“聚焦过渡帧内改 value 被 TextField 内部状态吞掉”导致点击不生效。
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) {
        if (isFocused && clearOnFocus) {
            onValueChange("")
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = TextDim) } },
        modifier = modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = PanelBorder,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = TextDim,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

/**
 * 全局页面背景：射手座 + 雅典娜圣斗士主题底图。
 * 固定铺满视口（不随内容滚动），仅顶部保留少量压暗保证标题可读，
 * 底部渐隐到 Navy 与原页面底色自然衔接。
 *
 * 用法：把页面内容包进 [AppBackground] 的 content 即可。
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val customUri by UserPrefs.bgUri.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        if (customUri != null) {
            // 用户自定义背景图片：铺满裁剪，并叠加压暗渐变保证文字可读
            Image(
                painter = rememberAsyncImagePainter(
                    model = customUri,
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.bg_sagittarius_athena)
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Navy.copy(alpha = 0.35f),
                        0.2f to Navy.copy(alpha = 0.1f),
                        0.7f to Navy.copy(alpha = 0.3f),
                        1f to Navy.copy(alpha = 0.92f)
                    )
                )
            )
        } else {
            // 默认射手座 + 雅典娜圣斗士主题底图
            Image(
                painter = painterResource(id = R.drawable.bg_sagittarius_athena),
                contentDescription = null,
                // Fit 完整呈现两位人物，避免窄屏下被裁掉两侧
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .align(Alignment.TopCenter)
                    .alpha(0.95f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Navy.copy(alpha = 0.45f),
                            0.18f to Navy.copy(alpha = 0.16f),
                            0.64f to Navy.copy(alpha = 0.22f),
                            0.86f to Navy.copy(alpha = 0.72f),
                            1f to Navy.copy(alpha = 0.96f)
                        )
                    )
            )
        }
        content()
    }
}
