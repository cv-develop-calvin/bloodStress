package org.bp.songbaobao.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 药品购买跳转。
 *
 * 优先唤起「美团」App，失败则退回浏览器打开美团买药搜索页，
 * 再失败则用系统搜索。全程不依赖任何 SDK，仅用 Intent。
 */
object MedPurchase {

    /** 美团包名 */
    private const val MEITUAN_PKG = "com.sankuai.meituan"

    /** 美团买药网页版搜索地址 */
    private fun webUrl(keyword: String): String =
        "https://i.meituan.com/awp/h5/health/search/index.html?keyword=${enc(keyword)}"

    /**
     * 发起购买：点击药品卡片上的「购买」按钮时调用。
     * @param name 药品名
     * @param dosage 规格，如 "10mg"，用于提高搜索准确度
     */
    fun buy(context: Context, name: String, dosage: String = "") {
        if (name.isBlank()) {
            Toast.makeText(context, "药品名称为空", Toast.LENGTH_SHORT).show()
            return
        }
        val keyword = buildString {
            append(name.trim())
            if (dosage.isNotBlank()) append(' ').append(dosage.trim())
        }

        // 1) 尝试直接唤起美团 App
        if (openApp(context, keyword)) return
        // 2) 退回浏览器打开美团买药搜索页
        if (openWeb(context, keyword)) return
        // 3) 最后兜底：系统搜索
        openSystemSearch(context, keyword)
    }

    private fun openApp(context: Context, keyword: String): Boolean {
        return try {
            // 先用包名做一次可见性判断，避免直接抛异常
            context.packageManager.getLaunchIntentForPackage(MEITUAN_PKG) ?: return false
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl(keyword))).apply {
                setPackage(MEITUAN_PKG)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun openWeb(context: Context, keyword: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl(keyword))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun openSystemSearch(context: Context, keyword: String) {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", "$keyword 购买")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            Toast.makeText(context, "未找到可用的购买渠道", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enc(s: String): String = Uri.encode(s)
}
