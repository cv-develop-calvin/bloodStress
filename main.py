"""Android 入口：在手机本机启动 Web 服务，供 WebView / 浏览器访问。

- 用 Buildozer(python-for-android) 的 webview bootstrap 打包时，APK 启动后
  会自动加载 http://127.0.0.1:5000/（端口与打包参数 --port 一致，默认 5000）。
- 在 Termux 或桌面 Python 中直接运行时，打印访问地址并保持常驻。
"""
from __future__ import annotations

import os
import sys
import threading
import time

import storage
from app import app

PORT = int(os.environ.get("BP_PORT", "5000"))


def serve() -> None:
    storage.init_db()
    app.run(host="127.0.0.1", port=PORT, debug=False, threaded=True, use_reloader=False)


def main() -> None:
    threading.Thread(target=serve, daemon=True).start()
    print(f"血压记录服务已启动： http://127.0.0.1:{PORT}", flush=True)
    print(f"数据库：{storage.db_path()}", flush=True)
    try:
        while True:
            time.sleep(3600)
    except KeyboardInterrupt:
        print("已退出")


if __name__ == "__main__":
    main()
