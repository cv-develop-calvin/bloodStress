#!/data/data/com.termux/files/usr/bin/bash
# 在安卓手机 Termux 中直接运行本应用（无需打包 APK）
# 用法：把项目复制到手机后执行  bash run_android.sh
set -e

cd "$(dirname "$0")"

pkg install -y python
python -m pip install --upgrade pip
python -m pip install flask

export BP_PORT=5000
python main.py
# 启动后在手机浏览器打开 http://127.0.0.1:5000 ，或直接用 Termux:Widget 建桌面快捷方式
