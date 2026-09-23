package org.bp.songbaobao.ui.navigation

/** 底部导航与路由定义 */
sealed class Screen(
    val route: String,
    val label: String,
    val icon: String,
    val showInBar: Boolean = true
) {
    object Bp : Screen("bp", "血压", "♈")
    object Med : Screen("med", "用药", "💊")
    object Lab : Screen("lab", "血常规", "🩸")
    object Notes : Screen("notes", "笔记本", "📖")
    object About : Screen("about", "版本", "⚙️")

    // 二级页面（不显示在底栏）
    object BpAdd : Screen("bp/add", "添加血压", "", false)
    object BpEdit : Screen("bp/edit/{id}", "编辑血压", "", false)
    object BpList : Screen("bp/list", "全部记录", "", false)
    object MedAdd : Screen("med/add", "添加药品", "", false)
    object MedEdit : Screen("med/edit/{id}", "编辑药品", "", false)
    object LabAdd : Screen("lab/add", "添加血常规", "", false)
    object LabEdit : Screen("lab/edit/{id}", "编辑血常规", "", false)
    object LabScan : Screen("lab/scan", "拍照识别", "", false)
    object NoteAdd : Screen("note/add", "写留言", "", false)
    object NoteEdit : Screen("note/edit/{id}", "编辑留言", "", false)
    // 用 note/detail/{id} 而非 note/{id}：后者会与 note/edit/{id} 前缀冲突，
    // 导航时可能把 "edit" 当作 id 解析而抛异常。
    object NoteDetail : Screen("note/detail/{id}", "留言详情", "", false)

    companion object {
        // 必须用 get() 而非 val 初始化：
        // companion object 的静态初始化早于外部类各 object 单例的初始化，
        // 若写成 val barItems = listOf(Bp, ...)，此处 Bp 等仍为 null，
        // 会导致底部栏渲染时 NPE 崩溃。
        val barItems: List<Screen>
            get() = listOf(Bp, Med, Lab, Notes, About)

        fun bpEdit(id: Long) = "bp/edit/$id"
        fun medEdit(id: Long) = "med/edit/$id"
        fun labEdit(id: Long) = "lab/edit/$id"
        fun noteEdit(id: Long) = "note/edit/$id"
        fun noteDetail(id: Long) = "note/detail/$id"
    }
}
