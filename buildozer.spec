[app]
title = 宋宝宝的记录
package.name = songbaobao
package.domain = org.bp

source.dir = .
# jpg/jpeg 必须包含：主题背景图是 JPG，漏掉会导致 APK 内缺图
source.include_exts = py,png,jpg,jpeg,svg,gif,webp,html,css,js,json,db,txt,md
source.exclude_dirs = data,__pycache__,.buildozer,bin,dist,shots,.git,.github

# 版本号须与 version.py 的 VERSION 保持一致
version = 1.1.0

# pillow 用于笔记照片的压缩与缩略图；flask 依赖 markupsafe/jinja2
requirements = python3,flask,markupsafe,jinja2,werkzeug,itsdangerous,click,pillow

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
