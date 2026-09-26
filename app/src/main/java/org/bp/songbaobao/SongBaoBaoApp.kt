package org.bp.songbaobao

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.bp.songbaobao.util.UserPrefs

@HiltAndroidApp
class SongBaoBaoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 尽早安装，确保启动阶段的崩溃也能被记录
        CrashHandler.install(this)
        // 加载用户个性化设置（系统色系 / 背景图片 / 自定义标题）
        UserPrefs.init(this)
    }
}
