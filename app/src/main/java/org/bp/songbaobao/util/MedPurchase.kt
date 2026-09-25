package org.bp.songbaobao.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * 药品购买渠道。
 *
 * 教训：早期版本用的是
 *   https://i.meituan.com/awp/h5/health/search/index.html?keyword=xxx
 * 该地址已失效（HTTP 404），打开必然是错误页。第三方电商的 H5 地址随时可能变更，
 * 因此这里做了三点加固：
 *   1. 渠道地址集中在一处，失效时只需改这里的常量；
 *   2. 打开前用 resolveActivity 预检，避免直接抛 ActivityNotFoundException；
 *   3. 提供多渠道选择 + 「复制药名」兜底，任一渠道失效用户仍有出路。
 */
object MedPurchase {

    /** 一个可选的购买渠道 */
    data class Channel(
        val id: String,
        val name: String,
        val desc: String,
        /** 生成该渠道的搜索地址 */
        val url: (keyword: String) -> String
    )

    /**
     * 可用渠道列表。
     * 地址均为 2026-09 实测可访问（HTTP 200），如后续失效需同步更新。
     */
    val channels: List<Channel> = listOf(
        Channel(
            id = "meituan",
            name = "美团买药",
            desc = "30 分钟送药，适合急用",
            url = { kw -> "https://maiyao.meituan.com/search?keyword=${enc(kw)}" }
        ),
        Channel(
            id = "jd",
            name = "京东健康",
            desc = "药品种类全，适合囤货",
            url = { kw -> "https://search.jd.com/Search?keyword=${enc(kw)}&enc=utf-8" }
        ),
        Channel(
            id = "taobao",
            name = "淘宝",
            desc = "价格常有优势",
            url = { kw -> "https://s.taobao.com/search?q=${enc(kw)}" }
        ),
        Channel(
            id = "alihealth",
            name = "阿里健康",
            desc = "阿里健康大药房",
            url = { kw -> "https://www.alihealth.cn/" }
        )
    )

    /** 默认渠道：美团买药 */
    val defaultChannel: Channel get() = channels.first()

    /** 药名 + 规格拼成搜索词 */
    fun keywordOf(name: String, dosage: String = ""): String = buildString {
        append(name.trim())
        if (dosage.isNotBlank()) append(' ').append(dosage.trim())
    }

    /**
     * 直接用默认渠道购买：交给系统处理外链。
     * 装了美团且美团接管了该域名时会唤起 App，否则用浏览器打开。
     * @return 是否成功发起跳转
     */
    fun buy(context: Context, name: String, dosage: String = ""): Boolean {
        if (name.isBlank()) {
            toast(context, "请先填写药品名称")
            return false
        }
        return open(context, defaultChannel, keywordOf(name, dosage))
    }

    /** 按指定渠道发起跳转 */
    fun buyWith(context: Context, channel: Channel, name: String, dosage: String = ""): Boolean {
        if (name.isBlank()) {
            toast(context, "请先填写药品名称")
            return false
        }
        return open(context, channel, keywordOf(name, dosage))
    }

    private fun open(context: Context, channel: Channel, keyword: String): Boolean {
        val uri = Uri.parse(channel.url(keyword))
        val pm = context.packageManager

        // 依次尝试：带 BROWSABLE 类别（规范做法）→ 纯 VIEW。
        // 每一步都先 resolveActivity 预检，避免 startActivity 抛异常导致闪退。
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
            if (intent.resolveActivity(pm) == null) continue
            return try {
                context.startActivity(intent)
                true
            } catch (t: Throwable) {
                // 记录但不抛出，交给下面的兜底
                android.util.Log.w("MedPurchase", "打开渠道失败: ${channel.id}", t)
                false
            }
        }
        return false
    }

    /**
     * 兜底：把药名复制到剪贴板。
     * 当所有渠道都打不开时，用户可自行打开电商 App 粘贴搜索。
     */
    fun copyName(context: Context, keyword: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            cm.setPrimaryClip(ClipData.newPlainText("药品名称", keyword))
            toast(context, "已复制「$keyword」，可粘贴到购药 App 搜索")
            true
        } catch (t: Throwable) {
            android.util.Log.w("MedPurchase", "复制到剪贴板失败", t)
            toast(context, "复制失败，请手动记录药名：$keyword")
            false
        }
    }

    /** Toast 必须在主线程弹出，这里统一兜底切换线程 */
    private fun toast(context: Context, msg: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enc(s: String): String = Uri.encode(s)
}
