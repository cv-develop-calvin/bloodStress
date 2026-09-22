package org.bp.songbaobao.data.repository

import android.content.pm.PackageManager
import org.bp.songbaobao.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val packageName: String
)

@Singleton
class VersionRepository @Inject constructor() {

    fun current(): VersionInfo = VersionInfo(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        buildType = BuildConfig.BUILD_TYPE,
        packageName = BuildConfig.APPLICATION_ID
    )
}
