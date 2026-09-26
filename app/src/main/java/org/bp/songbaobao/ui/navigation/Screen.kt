package org.bp.songbaobao.ui.navigation

import androidx.annotation.StringRes
import org.bp.songbaobao.R

/**
 * 底部导航与路由定义。
 *
 * 标签改用字符串资源 id（labelRes）而非硬编码文本，
 * 这样切换语言时底栏能跟着变；label 保留为回退文案。
 */
sealed class Screen(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: String,
    val showInBar: Boolean = true
) {
    object Bp : Screen("bp", R.string.nav_bp, "♈")
    object Med : Screen("med", R.string.nav_med, "💊")
    object Lab : Screen("lab", R.string.nav_lab, "🩸")
    object Notes : Screen("notes", R.string.nav_notes, "📖")
    object About : Screen("about", R.string.nav_about, "⚙️")

    // 会员订阅（独立底栏标签）
    object Subscription : Screen("subscription", R.string.nav_membership, "👑")

    // 血糖（与血压并列的健康监测功能）
    object Glucose : Screen("glucose", R.string.nav_glucose, "🍬")
    object GlucoseList : Screen("glucose/all", R.string.title_all_records, "", false)
    object GlucoseAdd : Screen("glucose/add", R.string.title_add_glucose, "", false)
    object GlucoseEdit : Screen("glucose/edit/{id}", R.string.title_edit_glucose, "", false)
    object GlucoseScan : Screen("glucose/scan", R.string.glucose_scan_title, "", false)

    // 二级页面（不显示在底栏）
    object BpAdd : Screen("bp/add", R.string.title_add_bp, "", false)
    object BpEdit : Screen("bp/edit/{id}", R.string.title_edit_bp, "", false)
    object BpList : Screen("bp/list", R.string.title_all_records, "", false)
    object BpScan : Screen("bp/scan", R.string.bp_scan_title, "", false)
    object MedAdd : Screen("med/add", R.string.title_add_med, "", false)
    object MedEdit : Screen("med/edit/{id}", R.string.title_edit_med, "", false)
    object LabAdd : Screen("lab/add", R.string.title_add_lab, "", false)
    object LabEdit : Screen("lab/edit/{id}", R.string.title_edit_lab, "", false)
    object LabScan : Screen("lab/scan", R.string.title_scan, "", false)
    object NoteAdd : Screen("note/add", R.string.title_add_note, "", false)
    object NoteEdit : Screen("note/edit/{id}", R.string.title_edit_note, "", false)
    // 用 note/detail/{id} 而非 note/{id}：后者会与 note/edit/{id} 前缀冲突，
    // 导航时可能把 "edit" 当作 id 解析而抛异常。
    object NoteDetail : Screen("note/detail/{id}", R.string.title_note_detail, "", false)

    // 合规文档（应用商店上架必需，从关于页进入）
    object Legal : Screen("legal", R.string.title_legal, "", false)

    // 个性化设置（从关于页进入）
    object Settings : Screen("settings", R.string.settings_title, "", false)

    companion object {
        // 必须用 get() 而非 val 初始化：
        // companion object 的静态初始化早于外部类各 object 单例的初始化，
        // 若写成 val barItems = listOf(Bp, ...)，此处 Bp 等仍为 null，
        // 会导致底部栏渲染时 NPE 崩溃。
        val barItems: List<Screen>
            get() = listOf(Bp, Glucose, Med, Lab, Notes, Subscription, About)

        fun bpEdit(id: Long) = "bp/edit/$id"
        fun medEdit(id: Long) = "med/edit/$id"
        fun labEdit(id: Long) = "lab/edit/$id"
        fun noteEdit(id: Long) = "note/edit/$id"
        fun noteDetail(id: Long) = "note/detail/$id"
        fun glucoseEdit(id: Long) = "glucose/edit/$id"
    }
}
