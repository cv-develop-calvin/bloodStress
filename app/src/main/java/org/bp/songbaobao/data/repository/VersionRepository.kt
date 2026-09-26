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
import java.io.IOException
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

        // 下载相关常量
        private const val MAX_REDIRECTS = 5
        private const val DOWNLOAD_MAX_RETRY = 3
        private const val DOWNLOAD_CONNECT_TIMEOUT_MS = 20_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 60_000
        // 整体限时（含重定向、CDN 抖动与重试），避免个别机型卡在 GitHub 跳转/CDN 上无限等待
        private const val DOWNLOAD_TIMEOUT_MS = 5 * 60_000L
    }

    fun current(): VersionInfo = VersionInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        packageName = BuildConfig.APPLICATION_ID
    )

    /**
     * 查找已下载到缓存、且大小匹配的完整 APK（用于重新进入页面时复用，避免重复下载）。
     * 无匹配时返回 null。
     */
    fun cachedApk(context: Context, update: UpdateInfo): File? {
        val dir = File(context.cacheDir, APK_DIR)
        val target = File(dir, update.apkName.ifBlank { "update.apk" })
        return if (target.exists() && target.length() == update.apkSize && update.apkSize > 0) {
            target
        } else {
            null
        }
    }

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
        val target = File(dir, update.apkName.ifBlank { "update.apk" })

        // 已存在且大小匹配的完整包：直接复用，避免重复下载（解决“自动重新下载”）
        if (target.exists() && target.length() == update.apkSize && update.apkSize > 0) {
            onProgress(target.length(), target.length())
            return@withContext target
        }

        // 清理与本次版本无关 / 大小不符的旧包，避免堆积
        dir.listFiles()
            ?.filter { it != target && (it.name != update.apkName || update.apkSize <= 0) }
            ?.forEach { runCatching { it.delete() } }

        // 整体限时：GitHub Releases 的下载地址会 302 跳到 objects.githubusercontent.com 的
        // CDN，个别机型/网络下可能卡在跳转或慢速连接上，用独立线程 + Future 限时兜底。
        runWithTimeout(DOWNLOAD_TIMEOUT_MS) {
            downloadWithRetry(update, target, onProgress)
        }
    }

    /**
     * 失败重试：最多 [DOWNLOAD_MAX_RETRY] 次，退避后重试。
     * 借助断点续传，重试时从已下载字节处继续，而不是从头重下（解决“很慢”）。
     */
    private fun downloadWithRetry(
        update: UpdateInfo,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): File {
        var lastError: Throwable? = null
        repeat(DOWNLOAD_MAX_RETRY) { attempt ->
            try {
                return downloadOnce(update, target, onProgress)
            } catch (t: Throwable) {
                lastError = t
                // 半截文件保留（用于续传）；仅当整包已损坏且无法续传时才在下次覆盖
                if (attempt < DOWNLOAD_MAX_RETRY - 1) {
                    // 退避：指数增长，给网络/CDN 一点恢复时间
                    Thread.sleep(800L * (attempt + 1))
                }
            }
        }
        throw lastError ?: IOException(app.getString(R.string.msg_download_failed, ""))
    }

    /**
     * 单次下载：手动跟随重定向 + 断点续传（HTTP Range）。
     *
     * GitHub Releases 的 [UpdateInfo.apkUrl]（browser_download_url）会返回 302 跳到
     * objects.githubusercontent.com 的 CDN。部分 Android 的 HttpURLConnection 在跨主机
     * 302 下不会自动跟随，于是直接拿到 302 当作错误。这里把 instanceFollowRedirects 关掉，
     * 自己读 Location 头逐级跳转，确保最终落到真正的 APK 上。
     *
     * 若本地已有部分下载（断点续传），则从已下载字节处接着下载，避免每次都从头开始。
     */
    private fun downloadOnce(
        update: UpdateInfo,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): File {
        var url = update.apkUrl
        var conn: HttpURLConnection? = null
        // 已有字节（断点续传起点）；若大小未知则用写入模式从头开始
        val existing = if (target.exists() && update.apkSize > 0) target.length() else 0L
        val append = existing > 0
        try {
            repeat(MAX_REDIRECTS) { _ ->
                conn?.disconnect()
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = DOWNLOAD_CONNECT_TIMEOUT_MS
                    readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                    // 关闭自动跟随，自己处理跨主机跳转
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "SongBaoBao-Updater")
                    setRequestProperty("Accept", "*/*")
                    // 断点续传：只请求缺失的部分；服务器不支持时回退整包下载
                    if (append) setRequestProperty("Range", "bytes=$existing-")
                }
                conn!!.connect()
                val code = conn!!.responseCode
                if (code in 300..399) {
                    val location = conn!!.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        error(app.getString(R.string.msg_download_http, code))
                    }
                    url = location
                    return@repeat
                }
                if (code !in 200..299 && code != 206) {
                    error(app.getString(R.string.msg_download_http, code))
                }

                // 206=部分内容（续传）；200=整包。先判断服务器是否支持 Range。
                val supportRange = code == 206
                // 总大小：优先用 header（206 时 content-range 含总长），否则用已知 apkSize
                val total = if (supportRange) {
                    val cr = conn!!.getHeaderField("Content-Range")
                    cr?.substringAfterLast('/')?.toLongOrNull() ?: update.apkSize
                } else {
                    conn!!.contentLengthLong.takeIf { it > 0 } ?: update.apkSize
                }
                // 若服务器不支持 Range，丢弃已有半截文件，从头写
                val startAt = if (supportRange) existing else 0L
                // 续传起点进度先回调，避免进度条从 0 跳变
                onProgress(startAt, total)

                conn!!.inputStream.use { input ->
                    (if (startAt > 0L) java.io.FileOutputStream(target, true)
                     else target.outputStream()).use { output ->
                        val buf = ByteArray(256 * 1024)
                        var done = startAt
                        var lastReport = done
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            done += n
                            // 每 512KB 回调一次，避免频繁刷新 UI
                            if (done - lastReport >= 512 * 1024) {
                                lastReport = done
                                onProgress(done, total)
                            }
                        }
                        output.flush()
                        onProgress(done, total)
                        // 完整性校验：已知总大小时，下载量不足说明被中断
                        if (total > 0 && done < total) {
                            if (!supportRange) {
                                // 整包下载不完整且不能续传：删掉半截，下次重试从头
                                runCatching { target.delete() }
                            }
                            error(app.getString(R.string.msg_download_incomplete, done, total))
                        }
                    }
                }
                return target
            }
            error(app.getString(R.string.msg_download_redirect))
        } finally {
            conn?.disconnect()
        }
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
