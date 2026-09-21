"""应用版本信息：版本号来源、更新日志、版本比较。

版本号约定 `<major>.<minor>.<patch>`，同时给出 Android 的 versionCode
（major*10000 + minor*100 + patch），用于安装时判断新旧。
"""
from __future__ import annotations

import os
import re
import subprocess
from datetime import datetime

# 当前版本号（发版时改这里，并同步 buildozer.spec 的 version）
VERSION = "1.1.0"

APP_NAME = "宋宝宝的记录"
APP_NAME_EN = "songbaobao"
GITHUB_REPO = "cv-develop-calvin/bloodStress"

# 更新日志：新版本加到最前面
CHANGELOG = [
    {
        "version": "1.1.0",
        "date": "2026-09-22",
        "items": [
            "新增用药记录：药品档案、服药打卡、7 天依从性统计",
            "新增用药提醒：到点弹窗、响铃、震动与系统通知",
            "新增血常规记录：13 项常用指标与参考区间自动判定",
            "新增血常规趋势曲线（含参考区间色带）",
            "新增拍照识别化验单：手机拍照自动读取指标并预填",
            "新增血常规 CSV 导出",
            "应用图标改为猫咪照片",
        ],
    },
    {
        "version": "1.0.0",
        "date": "2026-09-20",
        "items": [
            "血压记录与趋势曲线（7/30/90 天 / 全部）",
            "血压分级判定与健康提示",
            "记录筛选、编辑、删除与 CSV 导出",
        ],
    },
]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))


def _read_file(path: str) -> str:
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


def version_code(version: str | None = None) -> int:
    """1.2.3 -> 10203。"""
    parts = [int(x) for x in re.findall(r"\d+", version or VERSION)[:3]]
    while len(parts) < 3:
        parts.append(0)
    return parts[0] * 10000 + parts[1] * 100 + parts[2]


def parse_version(text: str) -> tuple:
    parts = [int(x) for x in re.findall(r"\d+", text or "")[:3]]
    while len(parts) < 3:
        parts.append(0)
    return tuple(parts)


def compare(a: str, b: str) -> int:
    """a > b 返回 1，相等 0，小于 -1。"""
    pa, pb = parse_version(a), parse_version(b)
    return (pa > pb) - (pa < pb)


def git_info() -> dict:
    """读取本地 git 提交信息（打包后通常取不到，返回空）。"""
    def run(*args):
        try:
            out = subprocess.run(["git", *args], cwd=BASE_DIR, capture_output=True,
                                 text=True, timeout=5)
            return out.stdout.strip() if out.returncode == 0 else ""
        except (OSError, subprocess.SubprocessError):
            return ""

    sha = run("rev-parse", "--short", "HEAD")
    return {
        "sha": sha,
        "branch": run("rev-parse", "--abbrev-ref", "HEAD"),
        "date": run("log", "-1", "--format=%ad", "--date=format:%Y-%m-%d %H:%M"),
        "message": run("log", "-1", "--format=%s"),
        "remote": run("config", "--get", "remote.origin.url"),
    }


def build_info() -> dict:
    """构建信息：打包时由 CI 写入 build_info.json，本地运行时用 git 推断。"""
    import json

    path = os.path.join(BASE_DIR, "build_info.json")
    data = {}
    if os.path.exists(path):
        try:
            data = json.loads(_read_file(path) or "{}")
        except ValueError:
            data = {}
    git = git_info()
    return {
        "version": VERSION,
        "version_code": version_code(),
        "build_time": data.get("build_time") or datetime.now().strftime("%Y-%m-%d %H:%M"),
        "commit": data.get("commit") or git["sha"] or "—",
        "branch": data.get("branch") or git["branch"] or "—",
        "run_number": data.get("run_number") or "—",
        "build_type": data.get("build_type") or ("dev" if git["sha"] else "release"),
        "repo": data.get("repo") or git["remote"] or GITHUB_REPO,
    }


def repo_slug() -> str:
    """把 git 远端地址规范化成 owner/repo。"""
    git = git_info()
    text = git["remote"] or GITHUB_REPO
    m = re.search(r"github\.com[:/]+([^/]+)/([^/]+?)(?:\.git)?/?$", text)
    return f"{m.group(1)}/{m.group(2)}" if m else GITHUB_REPO


def latest_release(repo: str | None = None) -> dict | None:
    """查询 GitHub 最新 Release（有网络时用于检查更新）。失败返回 None。"""
    import json
    import urllib.request

    url = f"https://api.github.com/repos/{repo or repo_slug()}/releases/latest"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": APP_NAME_EN})
        with urllib.request.urlopen(req, timeout=4) as resp:
            data = json.load(resp)
    except Exception:  # noqa: BLE001  网络不可用很常见
        return None

    tag = (data.get("tag_name") or "").lstrip("vV")
    assets = [{"name": a.get("name"), "url": a.get("browser_download_url"),
               "size": a.get("size")} for a in data.get("assets", [])]
    return {
        "tag": tag,
        "name": data.get("name") or tag,
        "published_at": (data.get("published_at") or "")[:10],
        "notes": data.get("body") or "",
        "assets": assets,
        "html_url": data.get("html_url"),
    }


def check_update() -> dict:
    """对比当前版本与 GitHub 最新 Release。"""
    release = latest_release()
    slug = repo_slug()
    result = {
        "current": VERSION,
        "current_code": version_code(),
        "repo": slug,
        "release": release,
        "has_update": False,
        "newer": False,
        "message": "",
        "releases_url": f"https://github.com/{slug}/releases",
    }
    if not release:
        result["message"] = "未能连接到 GitHub（可能没联网），可稍后重试"
        return result
    if not release["tag"]:
        result["message"] = "远端还没有发布版本，先在 GitHub 上创建 Release 或用标签发版"
        return result

    result["newer"] = compare(VERSION, release["tag"]) > 0
    result["has_update"] = compare(release["tag"], VERSION) > 0
    if result["has_update"]:
        result["message"] = f"发现新版本 {release['tag']}"
    elif result["newer"]:
        result["message"] = f"本地版本（v{VERSION}）比远端发布（v{release['tag']}）更新"
    else:
        result["message"] = f"已是最新版本 v{VERSION}"
    return result
