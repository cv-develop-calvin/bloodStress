"""Android 入口：在手机本机启动 Web 服务，供 WebView / 浏览器访问。

- 用 Buildozer(python-for-android) 的 webview bootstrap 打包时，APK 启动后
  会自动加载 http://127.0.0.1:5000/（端口与打包参数 --port 一致，默认 5000）。
- 在 Termux 或桌面 Python 中直接运行时，打印访问地址并保持常驻。
"""
from __future__ import annotations

import atexit
import os
import sys
import threading
import time

import storage
import version
from app import app

PORT = int(os.environ.get("BP_PORT", "5000"))
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STARTUP_LOG = os.path.join(storage._data_dir(), "startup.log")


def _log(message: str) -> None:
    """同时输出到控制台与文件，便于无法看控制台时排查崩溃。"""
    line = f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] {message}"
    print(line, flush=True)
    try:
        os.makedirs(os.path.dirname(STARTUP_LOG), exist_ok=True)
        with open(STARTUP_LOG, "a", encoding="utf-8") as f:
            f.write(line + "\n")
    except OSError:
        pass


def serve() -> None:
    storage.init_db()
    _log(f"启动服务 版本 v{version.VERSION} 端口 {PORT}")
    app.run(host="127.0.0.1", port=PORT, debug=False, threaded=True, use_reloader=False)


def main() -> None:
    try:
        info = version.build_info()
        _log(f"宋宝宝的记录 v{info['version']} (build {info['version_code']}, "
             f"{info['build_type']}, commit {info['commit']})")
        _log(f"数据库：{storage.db_path()}")
        threading.Thread(target=serve, daemon=True).start()
        _log(f"服务地址： http://127.0.0.1:{PORT}")
        while True:
            time.sleep(3600)
    except KeyboardInterrupt:
        _log("已退出")
    except Exception as exc:  # noqa: BLE001 启动阶段兜底，避免静默闪退
        import traceback
        _log("启动失败：" + repr(exc))
        _log(traceback.format_exc())
        raise
    finally:
        atexit._run_exitfuncs()


if __name__ == "__main__":
    main()
