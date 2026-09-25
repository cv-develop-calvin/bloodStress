package org.bp.songbaobao.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * 药品购买跳转。
 *
 * 用统一的外链 Intent 交给系统处理：
 * - 手机上装了美团且美团接管了该域名，会直接唤起美团原生页面；
 * - 否则由浏览器打开美团买药搜索页；
 * - 都没有时用系统搜索兜底。
 *
 * 全程不依赖任何 SDK，也不强制指定包名打开网页（美团内打开外链 H5 容易出现错误页）。
 */
object MedPurchase {

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
            toast(context, "请先填写药品名称")
            return
        }
        val keyword = buildString {
            append(name.trim())
            if (dosage.isNotBlank()) append(' ').append(dosage.trim())
        }

        // 1) 交给系统：美团若能接管该链接会被直接唤起，否则由浏览器打开
        if (openExternal(context, keyword)) return
        // 2) 最后兜底：系统搜索
        if (openSystemSearch(context, keyword)) return
        toast(context, "未找到可用的购买渠道，请手动搜索「$keyword」")
    }

    private fun openExternal(context: Context, keyword: String): Boolean {
        val uri = Uri.parse(webUrl(keyword))
        val pm = context.packageManager
        // 先带 CATEGORY_BROWSABLE（规范做法），不行再退回不带类别的纯 VIEW
        val candidates = listOf(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        for (intent in candidates) {
            val resolved = intent.resolveActivity(pm) != null
            if (!resolved) continue
            return try {
                context.startActivity(intent)
                true
            } catch (_: Throwable) {
                false
            }
        }
        return false
    }

    private fun openSystemSearch(context: Context, keyword: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", "$keyword 购买")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) == null) return false
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Toast 必须在主线程弹出，这里统一兜底切换线程 */
    private fun toast(context: Context, msg: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enc(s: String): String = Uri.encode(s)
}
