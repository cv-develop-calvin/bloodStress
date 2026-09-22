package org.bp.songbaobao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.bp.songbaobao.R

/**
 * 应用统一使用本地 Noto Sans SC 字体（res/font），
 * 避免各手机厂商系统字体差异导致字形不一致。
 * 注意：字体为静态实例化字重，必须按字重分别引用。
 */
val NotoSansScFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold, FontWeight.Bold)
)

private val AppTypography = Typography().run {
    val base = { size: Int, weight: FontWeight, lh: Int ->
        TextStyle(
            fontFamily = NotoSansScFamily,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lh.sp
        )
    }
    copy(
        displayLarge = base(57, FontWeight.Bold, 64),
        displayMedium = base(45, FontWeight.Bold, 52),
        displaySmall = base(36, FontWeight.Bold, 44),
        headlineLarge = base(32, FontWeight.Bold, 40),
        headlineMedium = base(28, FontWeight.Bold, 36),
        headlineSmall = base(24, FontWeight.Bold, 32),
        titleLarge = base(22, FontWeight.Medium, 28),
        titleMedium = base(18, FontWeight.Medium, 24),
        titleSmall = base(14, FontWeight.Medium, 20),
        bodyLarge = base(16, FontWeight.Normal, 24),
        bodyMedium = base(14, FontWeight.Normal, 20),
        bodySmall = base(12, FontWeight.Normal, 16),
        labelLarge = base(14, FontWeight.Medium, 20),
        labelMedium = base(12, FontWeight.Medium, 16),
        labelSmall = base(11, FontWeight.Medium, 16)
    )
}

/**
 * 深色为主、金色点缀的圣斗士配色。
 * 本应用固定深色主题（星夜背景），故不提供浅色方案。
 */
private val SaintDarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    primaryContainer = GoldDeep,
    onPrimaryContainer = GoldBright,

    secondary = ChartDiastolic,
    onSecondary = Navy,
    secondaryContainer = Violet,
    onSecondaryContainer = TextMain,

    tertiary = ChartPulse,
    onTertiary = Navy,

    background = Navy,
    onBackground = TextMain,
    surface = NavySoft,
    onSurface = TextMain,
    surfaceVariant = NavySoft,
    onSurfaceVariant = TextDim,

    surfaceContainer = PanelBg,
    surfaceContainerLow = NavySoft,
    surfaceContainerHigh = NavySoft,
    surfaceContainerHighest = Violet,

    outline = PanelBorder,
    outlineVariant = DividerGold,

    error = DangerRed,
    onError = Navy,
    errorContainer = Color(0x33FF8080),
    onErrorContainer = DangerRed
)

@Composable
fun SongBaoBaoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 无论系统设置如何都使用深色：本应用以星夜背景为设计基调
    MaterialTheme(
        colorScheme = SaintDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
