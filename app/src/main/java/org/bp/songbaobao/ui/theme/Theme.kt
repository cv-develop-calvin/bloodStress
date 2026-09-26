package org.bp.songbaobao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import org.bp.songbaobao.util.UserPrefs
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

/**
 * 可选的系统色系预设。每个预设仅在 SaintDarkColorScheme 基础上替换强调色，
 * 背景与文本颜色保持一致以保证可读性。
 */
data class ThemePreset(
    val id: String,
    val nameRes: Int,
    val swatch: Color,
    val scheme: ColorScheme
)

private fun accentScheme(primary: Color, secondary: Color, tertiary: Color): ColorScheme =
    SaintDarkColorScheme.copy(
        primary = primary,
        onPrimary = Navy,
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Navy,
        secondaryContainer = secondary.copy(alpha = 0.22f),
        onSecondaryContainer = TextMain,
        tertiary = tertiary,
        onTertiary = Navy
    )

val ThemePresets = listOf(
    ThemePreset("saint", R.string.theme_saint, GoldBright, SaintDarkColorScheme),
    ThemePreset(
        "ocean", R.string.theme_ocean, Color(0xFF5BC8F5),
        accentScheme(Color(0xFF5BC8F5), Color(0xFF7FE3C0), Color(0xFFA99CFF))
    ),
    ThemePreset(
        "forest", R.string.theme_forest, Color(0xFF7FD98A),
        accentScheme(Color(0xFF7FD98A), Color(0xFFBFE3A0), Color(0xFFFFE9A8))
    ),
    ThemePreset(
        "rose", R.string.theme_rose, Color(0xFFFF9BB0),
        accentScheme(Color(0xFFFF9BB0), Color(0xFFFFC46E), Color(0xFFA99CFF))
    ),
    ThemePreset(
        "violet", R.string.theme_violet, Color(0xFFB79CFF),
        accentScheme(Color(0xFFB79CFF), Color(0xFF7FE3C0), Color(0xFFFFDD7A))
    ),
    ThemePreset(
        "amber", R.string.theme_amber, Color(0xFFFFC46E),
        accentScheme(Color(0xFFFFC46E), Color(0xFFFFDD7A), Color(0xFFFF9BB0))
    )
)

fun schemeFor(id: String): ColorScheme =
    ThemePresets.firstOrNull { it.id == id }?.scheme ?: SaintDarkColorScheme

@Composable
fun SongBaoBaoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 无论系统设置如何都使用深色：本应用以星夜背景为设计基调
    // 强调色由用户设置的系统色系决定，即时生效
    val schemeId by UserPrefs.themeScheme.collectAsState()
    MaterialTheme(
        colorScheme = schemeFor(schemeId),
        typography = AppTypography,
        content = content
    )
}
