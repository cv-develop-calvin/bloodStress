package org.bp.songbaobao

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获：把未捕获异常的堆栈写入应用私有目录 crash/last_crash.txt，
 * 便于在无法连接调试器时定位闪退原因。
 * 应用下次启动时由关于页读取展示。
 */
object CrashHandler {

    private const val DIR = "crash"
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                write(app, throwable)
            } catch (_: Throwable) {
                // 记录失败时忽略，避免二次崩溃
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
        // 崩溃日志会被分享出去，用中英双语都能看懂的键名，
        // 且随应用语言切换（这里统一用英文键名，避免分享给开发者时乱码歧义）
        val header = buildString {
            appendLine(context.getString(R.string.crash_time, time))
            appendLine(context.getString(R.string.crash_device, Build.MANUFACTURER, Build.MODEL))
            appendLine(
                context.getString(
                    R.string.crash_os, Build.VERSION.RELEASE, Build.VERSION.SDK_INT
                )
            )
            appendLine(
                context.getString(
                    R.string.crash_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE
                )
            )
            appendLine("---------------- stack ----------------")
        }
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        File(dir, FILE).writeText(header + sw.toString())
    }

    /** 读取上次崩溃信息，无则返回 null */
    fun read(context: Context): String? {
        val f = File(File(context.filesDir, DIR), FILE)
        return if (f.exists()) f.readText() else null
    }

    fun clear(context: Context) {
        File(File(context.filesDir, DIR), FILE).delete()
    }

    /** 以文本分享崩溃日志 */
    fun shareIntent(context: Context, text: String): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_share_subject))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(
            intent,
            context.getString(R.string.crash_share_title)
        ).apply {
            // 从非 Activity 上下文启动需要新任务
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** 当前进程 id，用于日志 */
    fun pid(): Int = Process.myPid()
}
