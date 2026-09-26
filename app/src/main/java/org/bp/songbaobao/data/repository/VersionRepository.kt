package org.bp.songbaobao.data.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bp.songbaobao.BuildConfig
import org.bp.songbaobao.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val packageName: String
)

/** GitHub 上发现的可用更新 */
data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val versionCode: Int,
    val publishedAt: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long
) {
    /** 相对于当前安装版本的体积展示 */
    fun sizeText(): String = when {
        apkSize <= 0 -> ""
        apkSize < 1024 * 1024 -> "%.0f KB".format(apkSize / 1024.0)
        else -> "%.1f MB".format(apkSize / 1024.0 / 1024.0)
    }
}

/** 检查结果 */
sealed interface UpdateCheck {
    /** 已是最新 */
    data class UpToDate(val currentVersion: String) : UpdateCheck
    /** 有新版本 */
    data class Available(val update: UpdateInfo) : UpdateCheck
    /** 检查失败 */
    data class Failed(val reason: String) : UpdateCheck
}

@Singleton
class VersionRepository @Inject constructor(private val app: Application) {

    companion object {
        /** 仓库地址与 Releases API（公开仓库，匿名可读） */
        private const val REPO = "cv-develop-calvin/bloodStress"
        private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
        private const val TAG_PREFIX = "build-"
        private const val APK_DIR = "updates"
    }

    fun current(): VersionInfo = VersionInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        packageName = BuildConfig.APPLICATION_ID
    )

    // ---------------- 检查更新 ----------------

    suspend fun checkForUpdate(): UpdateCheck = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i("VersionRepo", "进入 checkForUpdate，准备请求")
            // HttpURLConnection 是阻塞式 I/O，connectionTimeout 不约束 DNS 解析，
            // 协程的 withTimeout 也无法打断阻塞调用（网络异常时可能卡住数分钟）。
            // 因此用独立线程 + Future.get(timeout) 强制限时，超时直接放弃。
            val json = runWithTimeout(15_000) { httpGetText(API_LATEST) }
            val root = JSONObject(json)

            val tag = root.optString("tag_name")
            // CI 会把构建错误日志以 pre-release 形式发布，只接受 build-<数字> 形式的正式构建
            if (!tag.startsWith(TAG_PREFIX)) {
                return@withContext UpdateCheck.Failed(app.getString(R.string.msg_not_app_build, tag))
            }
            val buildNo = tag.removePrefix(TAG_PREFIX).toIntOrNull()
                ?: return@withContext UpdateCheck.Failed(app.getString(R.string.msg_bad_build_number, tag))

            val assets = root.optJSONArray("assets") ?: JSONArray()
            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = a.optString("browser_download_url")
                    apkName = name
                    apkSize = a.optLong("size")
                    break
                }
            }
            if (apkUrl.isBlank()) {
                return@withContext UpdateCheck.Failed(app.getString(R.string.msg_no_apk_link))
            }

            // 从 songbaobao-v1.2.0-build45.apk 解析出 1.2.0
            val remoteVersion = apkName
                .substringAfter("songbaobao-v", "")
                .substringBefore("-build")
                .ifBlank { "0.0.0" }

            // 按语义化版本比较（主.次.修订），比构建号直观且不受 CI 编号影响
            if (compareVersion(remoteVersion, BuildConfig.VERSION_NAME) <= 0) {
                return@withContext UpdateCheck.UpToDate(BuildConfig.VERSION_NAME)
            }

            UpdateCheck.Available(
                UpdateInfo(
                    tagName = tag,
                    versionName = remoteVersion,
                    versionCode = buildNo,
                    publishedAt = root.optString("published_at").take(10),
                    notes = root.optString("body").trim(),
                    apkUrl = apkUrl,
                    apkName = apkName,
                    apkSize = apkSize
                )
            )
        } catch (t: Throwable) {
            UpdateCheck.Failed(friendlyError(t))
        }
    }

    /**
     * 在独立线程执行阻塞任务并限时等待。
     * 用于绕开「阻塞 I/O 无法被协程取消」的问题：超时后立即抛错，
     * 后台线程会在底层超时后自行结束（不会泄漏到用户可见的等待）。
     */
    private inline fun <T> runWithTimeout(timeoutMs: Long, crossinline block: () -> T): T {
        val pool = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            Thread(r, "version-check").apply { isDaemon = true }
        }
        return try {
            pool.submit(java.util.concurrent.Callable { block() })
                .get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            throw java.net.SocketTimeoutException(app.getString(R.string.msg_timeout))
        } catch (e: java.util.concurrent.ExecutionException) {
            throw (e.cause ?: e)
        } finally {
            pool.shutdownNow()
        }
    }

    /** 语义化版本比较：a > b 返回正数，相等返回 0，a < b 返回负数 */
    private fun compareVersion(a: String, b: String): Int {
        val pa = a.split('.', '-').mapNotNull { it.toIntOrNull() }
        val pb = b.split('.', '-').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }

    // ---------------- 下载 ----------------

    /**
     * 下载 APK 到应用私有缓存目录。
     * @param onProgress 已下载字节 / 总字节（总长未知时为 -1）
     * @return 下载好的文件
     */
    suspend fun downloadApk(
        context: Context,
        update: UpdateInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, APK_DIR).apply { mkdirs() }
        // 下载前清掉旧包，避免堆积占用空间
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, update.apkName.ifBlank { "update.apk" })

        val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            // 下载耗时较长，这里给宽松的读写超时；整体限时由调用方把控
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "SongBaoBao-Updater")
        }
        try {
            if (conn.responseCode !in 200..299) {
                error(app.getString(R.string.msg_download_http, conn.responseCode))
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.apkSize
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastReport = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        done += n
                        // 每 256KB 回调一次，避免频繁刷新 UI
                        if (done - lastReport >= 256 * 1024) {
                            lastReport = done
                            onProgress(done, total)
                        }
                    }
                    output.flush()
                    onProgress(done, total)
                }
            }
        } finally {
            conn.disconnect()
        }
        target
    }

    // ---------------- 安装 ----------------

    /** 用系统安装器打开已下载的 APK */
    fun installApk(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** 跳转到系统「安装未知应用」授权页（部分机型下载后需手动开启） */
    fun openInstallPermissionSettings(context: Context) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            // 忽略：个别机型无此页面
        }
    }

    // ---------------- HTTP ----------------

    private fun httpGetText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SongBaoBao-Updater")
        }
        try {
            if (conn.responseCode !in 200..299) {
                error("HTTP ${conn.responseCode}")
            }
            return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            conn.disconnect()
        }
    }

    /** 把网络异常翻译成用户能看懂的中文提示 */
    private fun friendlyError(t: Throwable): String = when (t) {
        is java.net.UnknownHostException -> app.getString(R.string.msg_net_unavailable)
        is java.net.SocketTimeoutException -> app.getString(R.string.msg_net_timeout)
        is java.net.ConnectException -> app.getString(R.string.msg_net_failed)
        else -> t.message ?: t.javaClass.simpleName
    }
}
