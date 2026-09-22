"""Android 入口：启动本地 Web 服务，供 WebView（APK）或浏览器（Termux）访问。

python-for-android 的 webview bootstrap 约定：
- 入口脚本（main.py）必须在**主线程**里以阻塞方式运行 HTTP 服务；
- 服务的 host/port 必须与打包参数 --port 一致（默认 5000，127.0.0.1）；
- 主线程返回会导致进程结束、WebView 加载失败。

启动过程中的任何异常都会写入 startup.log，便于无法连电脑时排查。
"""
from __future__ import annotations

import os
import sys
import time
import traceback

# ---------------------------------------------------------------- 日志
def _pick_log_dir() -> str:
    """挑一个一定可写的目录存启动日志。"""
    candidates = []
    for key in ("ANDROID_PRIVATE", "ANDROID_APP_PATH", "ANDROID_ARGUMENT"):
        value = os.environ.get(key)
        if value:
            candidates.append(value)
    here = os.path.dirname(os.path.abspath(__file__))
    candidates += [here, os.getcwd(), os.path.join("/sdcard", "SongBaobaoLog")]
    for path in candidates:
        try:
            os.makedirs(path, exist_ok=True)
            probe = os.path.join(path, ".probe")
            with open(probe, "w", encoding="utf-8") as f:
                f.write("ok")
            os.remove(probe)
            return path
        except Exception:  # noqa: BLE001
            continue
    return ""


LOG_DIR = _pick_log_dir()
LOG_FILE = os.path.join(LOG_DIR, "startup.log") if LOG_DIR else ""


def log(message: str) -> None:
    line = f"[{time.strftime('%H:%M:%S')}] {message}"
    try:
        print(line, flush=True)
    except Exception:  # noqa: BLE001 Android 上 stdout 可能不可用
        pass
    try:
        if LOG_FILE:
            with open(LOG_FILE, "a", encoding="utf-8") as f:
                f.write(time.strftime("[%Y-%m-%d ") + line + "\n")
    except Exception:  # noqa: BLE001
        pass


def log_env() -> None:
    keys = ("ANDROID_PRIVATE", "ANDROID_APP_PATH", "ANDROID_ARGUMENT",
            "PYTHONHOME", "PYTHONPATH", "TMPDIR")
    for k in keys:
        log(f"env {k} = {os.environ.get(k)!r}")
    log(f"python {sys.version.split()[0]} | cwd {os.getcwd()}")
    log(f"__file__ = {os.path.abspath(__file__)}")
    log(f"log dir = {LOG_DIR!r}")
    try:
        log("app files = " + str(sorted(os.listdir(os.path.dirname(os.path.abspath(__file__))))[:30]))
    except Exception as exc:  # noqa: BLE001
        log(f"listdir failed: {exc!r}")


# ---------------------------------------------------------------- 应用
def build_app():
    """构建 Flask app，并把导入期异常单独暴露出来。"""
    import storage
    import version
    from app import app

    storage.init_db()
    log(f"数据层就绪，db = {storage.db_path()}")
    log(f"版本 v{version.VERSION}")
    return app


def main() -> None:
    log("========== 启动 ==========")
    try:
        log_env()
    except Exception:  # noqa: BLE001 环境采集失败不能影响启动
        log("环境采集异常：\n" + traceback.format_exc())

    port = int(os.environ.get("BP_PORT", "5000"))

    try:
        application = build_app()
    except Exception:  # noqa: BLE001
        log("应用初始化失败：\n" + traceback.format_exc())
        raise

    log(f"开始监听 127.0.0.1:{port}")
    # 必须在主线程阻塞运行：WebView 会加载 http://127.0.0.1:<port>/
    # threaded=True 保证处理并发请求；use_reloader 在安卓上必须关闭。
    application.run(host="127.0.0.1", port=port, debug=False,
                    threaded=True, use_reloader=False)


if __name__ == "__main__":
    try:
        main()
    except BaseException:  # noqa: BLE001 记录后重新抛出，不吞异常
        log("致命错误：\n" + traceback.format_exc())
        raise
