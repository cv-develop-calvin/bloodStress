package org.bp.songbaobao.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户个性化设置：系统色系、背景图片、自定义系统标题。
 * 使用 SharedPreferences 持久化，并通过 StateFlow 暴露，方便 Compose 实时响应。
 */
object UserPrefs {

    private const val PREF_NAME = "user_prefs"
    private const val KEY_THEME = "theme_scheme"
    private const val KEY_BG = "bg_uri"
    private const val KEY_TITLE = "custom_title"

    private var prefs: android.content.SharedPreferences? = null

    private val _themeScheme = MutableStateFlow("saint")
    private val _bgUri = MutableStateFlow<String?>(null)
    private val _customTitle = MutableStateFlow<String?>(null)

    val themeScheme: StateFlow<String> = _themeScheme.asStateFlow()
    val bgUri: StateFlow<String?> = _bgUri.asStateFlow()
    val customTitle: StateFlow<String?> = _customTitle.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).also { p ->
            _themeScheme.value = p.getString(KEY_THEME, "saint") ?: "saint"
            _bgUri.value = p.getString(KEY_BG, null)
            _customTitle.value = p.getString(KEY_TITLE, null)?.takeIf { it.isNotBlank() }
        }
    }

    fun setThemeScheme(id: String) {
        _themeScheme.value = id
        prefs?.edit()?.putString(KEY_THEME, id)?.apply()
    }

    fun setBgUri(uri: String?) {
        _bgUri.value = uri
        prefs?.edit()?.putString(KEY_BG, uri)?.apply()
    }

    fun setCustomTitle(title: String?) {
        val t = title?.takeIf { it.isNotBlank() }
        _customTitle.value = t
        prefs?.edit()?.putString(KEY_TITLE, t)?.apply()
    }
}
