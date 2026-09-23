package org.bp.songbaobao

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SongBaoBaoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 尽早安装，确保启动阶段的崩溃也能被记录
        CrashHandler.install(this)
    }
}
