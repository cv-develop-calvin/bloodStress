[app]
title = 宋宝宝的记录
package.name = songbaobao
package.domain = org.bp

source.dir = .
# jpg/jpeg 必须包含：主题背景图是 JPG，漏掉会导致 APK 内缺图
source.include_exts = py,png,jpg,jpeg,svg,gif,webp,html,css,js,json,db,txt,md
source.exclude_dirs = data,__pycache__,.buildozer,bin,dist,shots,.git,.github,_apkcheck

# 版本号须与 version.py 的 VERSION 保持一致
version = 1.2.0

# 关键：显式锁定 Python 3.11。
# p4a 的 python3 recipe 默认会构建 3.14，而 flask 依赖的 markupsafe、
# 以及 pillow 都需要在本地编译 C 扩展，在 3.14 下 recipe 尚不成熟，
# 构建时会被静默跳过 —— 表现为 APK 装好一打开就闪退（ModuleNotFoundError）。
requirements = python3==3.11.5,hostpython3==3.11.5,flask,pillow

# WebView 模式：APK 内运行 Flask，界面由 Android WebView 显示
# WebView 访问的 localhost 端口由 p4a 的 --port 决定，默认为 5000，
# 必须与 main.py 里 app.run(port=...) 保持一致，所以两边都用 5000。
android.bootstrap = webview

android.permissions = INTERNET
# 只保留 64 位，避免旧架构编译失败、加快构建
android.archs = arm64-v8a

orientation = portrait
fullscreen = 0

icon.filename = static/img/icon-512.png
presplash.filename = static/img/icon-512.png
android.accept_sdk_license = True
android.allow_backup = True
android.release_artifacts = 1

[buildozer]
log_level = 2
warn_on_root = 1
