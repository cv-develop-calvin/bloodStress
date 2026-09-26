package org.bp.songbaobao.ui.screen.about

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bp.songbaobao.R
import org.bp.songbaobao.backup.BackupRepository
import org.bp.songbaobao.backup.ImportMode
import org.bp.songbaobao.data.repository.UpdateCheck
import org.bp.songbaobao.data.repository.UpdateInfo
import org.bp.songbaobao.data.repository.VersionRepository
import java.io.File
import javax.inject.Inject

/** 更新流程状态 */
data class UpdateUiState(
    /** 正在检查 */
    val checking: Boolean = false,
    /** 检查到的新版本 */
    val available: UpdateInfo? = null,
    /** 提示信息（已是最新 / 检查失败 / 下载完成等） */
    val message: String? = null,
    /** 正在下载 */
    val downloading: Boolean = false,
    /** 下载进度 0f..1f；总长未知时为 null（显示不确定进度条） */
    val progress: Float? = null,
    /** 下载完成的 APK 文件 */
    val downloadedApk: File? = null
)

/** 版本页状态：备份进行中 / 最近一次结果 */
data class AboutUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val detail: String? = null
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val backup: BackupRepository,
    private val version: VersionRepository,
    // 注入 Application 以便按当前语言取提示文案
    // （用 Application 而非 Activity，避免持有视图导致泄漏）
    private val app: android.app.Application
) : ViewModel() {

    private fun msg(resId: Int, vararg args: Any): String =
        app.getString(resId, *args)

    private val _state = MutableStateFlow(AboutUiState())
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    /** 备份文件名（用于导出对话框预填） */
    val defaultFileName: String get() = backup.defaultFileName()

    /** 当前版本号，用于界面展示 */
    val currentVersionName: String get() = version.current().versionName

    // ---------------- 备份 ----------------

    fun export(context: Context, target: Uri) {
        _state.value = AboutUiState(busy = true, message = msg(R.string.msg_exporting))
        viewModelScope.launch {
            val r = backup.export(context, target)
            _state.value = AboutUiState(busy = false, message = r.message, detail = r.counts)
        }
    }

    fun import(context: Context, source: Uri, mode: ImportMode) {
        _state.value = AboutUiState(busy = true, message = msg(R.string.msg_importing))
        viewModelScope.launch {
            val r = backup.import(context, source, mode)
            _state.value = AboutUiState(busy = false, message = r.message, detail = r.counts)
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, detail = null)
    }

    // ---------------- 更新 ----------------

    fun checkUpdate() {
        _update.value = UpdateUiState(checking = true)
        viewModelScope.launch {
            val r = try {
                version.checkForUpdate()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程取消（如页面销毁）直接向上抛，不做提示
                throw e
            } catch (t: Throwable) {
                UpdateCheck.Failed(t.message ?: t.javaClass.simpleName)
            }
            when (r) {
                is UpdateCheck.Available -> _update.value = UpdateUiState(available = r.update)
                is UpdateCheck.UpToDate ->
                    _update.value = UpdateUiState(
                        message = msg(R.string.msg_latest_version, r.currentVersion)
                    )
                is UpdateCheck.Failed ->
                    _update.value = UpdateUiState(
                        message = msg(R.string.msg_check_failed_reason, r.reason)
                    )
            }
        }
    }

    fun downloadUpdate(context: Context) {
        val info = _update.value.available ?: return
        _update.value = _update.value.copy(downloading = true, progress = 0f, message = null)
        viewModelScope.launch {
            try {
                val apk = version.downloadApk(context, info) { done, total ->
                    _update.value = _update.value.copy(
                        progress = if (total > 0) done.toFloat() / total else null
                    )
                }
                _update.value = _update.value.copy(
                    downloading = false,
                    progress = 1f,
                    downloadedApk = apk,
                    message = msg(R.string.msg_download_done_hint)
                )
            } catch (t: Throwable) {
                val reason = when (t) {
                    is java.net.UnknownHostException -> msg(R.string.msg_net_unavailable)
                    is java.net.SocketTimeoutException -> msg(R.string.msg_net_timeout)
                    is java.net.ConnectException -> msg(R.string.msg_net_failed)
                    else -> t.message ?: t.javaClass.simpleName
                }
                _update.value = _update.value.copy(
                    downloading = false,
                    progress = null,
                    message = msg(R.string.msg_download_failed, reason)
                )
            }
        }
    }

    /** 用系统安装器安装（应用内调用，失败时提示去开启未知来源安装权限） */
    fun install(context: Context) {
        val apk = _update.value.downloadedApk ?: return
        try {
            version.installApk(context, apk)
        } catch (t: Throwable) {
            _update.value = _update.value.copy(
                message = msg(
                    R.string.msg_installer_failed,
                    t.message ?: msg(R.string.msg_need_install_permission)
                )
            )
            version.openInstallPermissionSettings(context)
        }
    }

    /** 跳转系统设置，开启「安装未知应用」权限 */
    fun openInstallSettings(context: Context) = version.openInstallPermissionSettings(context)

    fun dismissUpdate() {
        _update.value = UpdateUiState()
    }
}
