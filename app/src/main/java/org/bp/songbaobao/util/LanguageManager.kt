package org.bp.songbaobao.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * 应用内语言切换。
 *
 * 只做「中文 / 英文 / 跟随系统」三态，偏好存在本机 SharedPreferences。
 * 实现方式：用 createConfigurationContext 包一层带目标 Locale 的 Context，
 * 在 Activity.attachBaseContext 注入，因此从 API 26 起所有版本行为一致，
 * 不依赖 Android 13 的 per-app language API（minSdk 26，很多设备没有该能力）。
 */
object LanguageManager {

    const val FOLLOW_SYSTEM = ""
    const val ZH = "zh"
    const val EN = "en"

    private const val PREFS = "language"
    private const val KEY = "app_language"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 当前保存的语言代码，空串表示跟随系统 */
    fun getSavedLanguage(context: Context): String =
        prefs(context).getString(KEY, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM

    /** 实际生效的语言代码（跟随系统时解析成具体语言） */
    fun currentLanguage(context: Context): String {
        val saved = getSavedLanguage(context)
        if (saved.isNotBlank()) return saved
        val sys = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return if (sys.language.startsWith("zh")) ZH else EN
    }

    fun setLanguage(context: Context, code: String) {
        prefs(context).edit().putString(KEY, code).apply()
    }

    /** 显示名（用各自语言自述，便于用户辨认） */
    fun displayName(code: String): String = when (code) {
        ZH -> "中文"
        EN -> "English"
        else -> "跟随系统"
    }

    /**
     * 生成带目标 Locale 的 Context。
     * 在 Activity.attachBaseContext 中调用 super.attachBaseContext(wrap(newBase))。
     */
    fun wrap(context: Context): Context {
        val code = getSavedLanguage(context)
        if (code.isBlank()) return context
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
