[app]
title = 宋宝宝的记录
package.name = songbaobao
package.domain = org.bp

source.dir = .
source.include_exts = py,png,jpg,svg,html,css,js,json,db,txt
source.exclude_dirs = data,__pycache__,.buildozer,bin

version = 1.0.0
requirements = python3,flask

# WebView 模式：APK 内运行 Flask，界面由 Android WebView 显示
android.bootstrap = webview

# WebView 默认加载 http://127.0.0.1:5000/ ，与 main.py 的 PORT 保持一致
android.permissions = INTERNET
# 只保留 64 位，避免旧架构编译失败、加快构建
android.archs = arm64-v8a

orientation = portrait
fullscreen = 0

icon.filename = static/img/icon-512.png
android.accept_sdk_license = True
android.allow_backup = True
# 需要签名发布版时改用：buildozer -v android release
android.release_artifacts = 1

[buildozer]
log_level = 2
warn_on_root = 1

# 打包命令（Linux / WSL / macOS 环境）：
#   pip install buildozer
#   buildozer -v android debug
# 产物：bin/<title>-<version>-debug.apk
