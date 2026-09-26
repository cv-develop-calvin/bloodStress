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
    private const val KEY_SUB_PLAN = "sub_plan"
    private const val KEY_SUB_ACTIVE = "sub_active"
    private const val KEY_SUB_EXPIRY = "sub_expiry"

    private var prefs: android.content.SharedPreferences? = null

    private val _themeScheme = MutableStateFlow("saint")
    private val _bgUri = MutableStateFlow<String?>(null)
    private val _customTitle = MutableStateFlow<String?>(null)
    private val _subPlan = MutableStateFlow<String?>(null)
    private val _subActive = MutableStateFlow(false)
    private val _subExpiry = MutableStateFlow(0L)

    val themeScheme: StateFlow<String> = _themeScheme.asStateFlow()
    val bgUri: StateFlow<String?> = _bgUri.asStateFlow()
    val customTitle: StateFlow<String?> = _customTitle.asStateFlow()
    val subscriptionPlan: StateFlow<String?> = _subPlan.asStateFlow()
    val subscriptionActive: StateFlow<Boolean> = _subActive.asStateFlow()
    val subscriptionExpiry: StateFlow<Long> = _subExpiry.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).also { p ->
            _themeScheme.value = p.getString(KEY_THEME, "saint") ?: "saint"
            _bgUri.value = p.getString(KEY_BG, null)
            _customTitle.value = p.getString(KEY_TITLE, null)?.takeIf { it.isNotBlank() }
            _subPlan.value = p.getString(KEY_SUB_PLAN, null)
            _subActive.value = p.getBoolean(KEY_SUB_ACTIVE, false)
            _subExpiry.value = p.getLong(KEY_SUB_EXPIRY, 0L)
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

    /** 记录订阅：套餐 id、是否生效、到期时间戳（0 表示未设置，Long.MAX_VALUE 表示永久）。 */
    fun setSubscription(plan: String?, active: Boolean, expiry: Long) {
        _subPlan.value = plan
        _subActive.value = active
        _subExpiry.value = expiry
        prefs?.edit()
            ?.putString(KEY_SUB_PLAN, plan)
            ?.putBoolean(KEY_SUB_ACTIVE, active)
            ?.putLong(KEY_SUB_EXPIRY, expiry)
            ?.apply()
    }

    /** 清除本地订阅标记（不会退款，仅清除本地状态）。 */
    fun clearSubscription() {
        setSubscription(null, false, 0L)
    }
}
