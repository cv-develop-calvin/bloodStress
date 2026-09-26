package org.bp.songbaobao.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 安全地打开外部链接（如 PayPal 支付页）。
 * 若设备没有可处理 ACTION_VIEW 的应用，则自动把链接复制到剪贴板并返回 false。
 */
object UrlLauncher {

    /** 尝试用外部浏览器打开链接；成功返回 true，失败（无可用应用）返回 false。 */
    fun openUrl(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                copy(context, url)
                false
            }
        } catch (e: Exception) {
            copy(context, url)
            false
        }
    }

    /** 复制文本到剪贴板，返回是否成功。 */
    fun copy(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("SongBaoBao", text))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 打开链接，失败时复制并 Toast 提示文案。 */
    fun openOrCopy(context: Context, url: String, failedMsg: String, copiedMsg: String) {
        val ok = openUrl(context, url)
        if (!ok) {
            val done = copy(context, url)
            Toast.makeText(
                context,
                if (done) copiedMsg else failedMsg,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
