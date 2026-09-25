package org.bp.songbaobao.util

import android.content.Context

/**
 * 隐私政策同意状态。
 *
 * 健康数据属于《个人信息保护法》定义的敏感个人信息，
 * 应用首次启动时必须先取得用户明示同意（《个人信息保护法》第 29 条），
 * 未同意前不应开始收集。这里只记录「是否已同意」这一个布尔值，
 * 且仅存于本机 SharedPreferences，不会上传。
 */
object PrivacyConsent {

    private const val PREFS = "privacy"
    private const val KEY_ACCEPTED = "policy_accepted"
    private const val KEY_ACCEPTED_AT = "policy_accepted_at"
    private const val KEY_VERSION = "policy_version"

    /** 政策版本号：政策内容有实质修改时递增，递增后需重新征得同意 */
    const val CURRENT_VERSION = 1

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 是否已同意当前版本的隐私政策 */
    fun isAccepted(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(KEY_ACCEPTED, false) &&
            p.getInt(KEY_VERSION, 0) == CURRENT_VERSION
    }

    /** 记录同意（含时间与政策版本） */
    fun accept(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ACCEPTED, true)
            .putInt(KEY_VERSION, CURRENT_VERSION)
            .putString(KEY_ACCEPTED_AT, nowStamp())
            .apply()
    }

    /** 同意时间，未同意则返回 null */
    fun acceptedAt(context: Context): String? =
        prefs(context).getString(KEY_ACCEPTED_AT, null)

    /** 撤回同意：清空标记，下次启动重新弹窗 */
    fun revoke(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
