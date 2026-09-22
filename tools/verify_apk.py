#!/usr/bin/env python3
"""检查 APK 是否包含运行所需的全部内容。

背景：python-for-android 的打包结构与源码目录完全不同 ——
  * 应用自身的 .py 会被编译成 .pyc，与 static/、templates/ 一起
    整体打包进 assets/private.tar
  * 第三方纯 Python 依赖（flask、jinja2、PIL 等）被打进
    lib/<arch>/libpybundle.so
因此不能在 APK 根目录找 .py 或 site-packages，必须分别解包检查。

用法：python tools/verify_apk.py bin/xxx.apk [...]
退出码：0 全部通过；1 有缺失（CI 会因此失败，避免产出装了就闪退的 APK）
"""
from __future__ import annotations

import io
import sys
import tarfile
import zipfile

# 应用私有目录内必须存在的文件（.pyc 与 .py 都接受）
REQUIRED_PY = {
    "应用入口 main": "main",
    "血压模块 app": "app",
    "笔记本模块 photos": "photos",
    "数据层 storage": "storage",
    "血常规 lab": "lab",
    "版本信息 version": "version",
}

REQUIRED_RES = {
    "主题背景 bg-saint.jpg": "bg-saint.jpg",
    "横幅 bg-saint-top.jpg": "bg-saint-top.jpg",
    "应用图标 icon-512.png": "icon-512.png",
    "字体 Noto Sans SC (woff2)": "NotoSansSC-subset.woff2",
    "字体 CSS": "css/fonts.css",
    "模板 base.html": "templates/base.html",
    "模板 notes.html": "templates/notes.html",
    "模板 lab.html": "templates/lab.html",
    "Chart.js": "chart.umd.min.js",
    "Bootstrap": "bootstrap.min.css",
}

# libpybundle.so 内应能找到的依赖特征字节
REQUIRED_LIBS = {
    "Flask": [b"flask/__init__", b"flask/app.py"],
    "Werkzeug": [b"werkzeug/"],
    "Jinja2": [b"jinja2/"],
    "markupsafe": [b"markupsafe"],
    "Pillow(PIL)": [b"PIL/Image", b"PIL/Image.py"],
}


def check_apk(path: str) -> list[str]:
    """返回缺失项列表，空列表表示通过。"""
    missing: list[str] = []
    with zipfile.ZipFile(path) as z:
        names = z.namelist()

        # ---- 1) 解包私有目录 ----
        tar_names: list[str] = []
        blob = b""
        if "assets/private.tar" in names:
            blob = z.read("assets/private.tar")
            with tarfile.open(fileobj=io.BytesIO(blob)) as t:
                tar_names = t.getnames()
        else:
            missing.append("私有目录 assets/private.tar")

        print(f"  私有目录文件数: {len(tar_names)}")

        def has_py(stem: str) -> bool:
            return any(n.endswith(stem + ".pyc") or n.endswith(stem + ".py")
                       for n in tar_names)

        def has_res(name: str) -> bool:
            return any(n.endswith(name) for n in tar_names)

        for label, stem in REQUIRED_PY.items():
            ok = has_py(stem)
            print(("  OK   " if ok else "  MISS ") + label)
            if not ok:
                missing.append(label)

        for label, name in REQUIRED_RES.items():
            ok = has_res(name)
            print(("  OK   " if ok else "  MISS ") + label)
            if not ok:
                missing.append(label)

        # ---- 2) 检查依赖库 ----
        bundles = [n for n in names if n.endswith("libpybundle.so")]
        if not bundles:
            print("  MISS 依赖包 libpybundle.so")
            missing.append("依赖包 libpybundle.so")
        else:
            data = z.read(bundles[0])
            print(f"  libpybundle.so 大小: {round(len(data) / 1048576, 1)} MB")
            for label, keys in REQUIRED_LIBS.items():
                ok = any(k in data for k in keys)
                print(("  OK   " if ok else "  MISS ") + f"{label}（bundle 内）")
                if not ok:
                    missing.append(label)

        # ---- 3) 附带信息 ----
        pylibs = [n for n in names if "libpython" in n]
        print(f"  Python 库: {pylibs}")
        abis = sorted({n.split("/")[1] for n in names if n.startswith("lib/")})
        print(f"  架构目录: {abis}")

    return missing


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print("用法: python tools/verify_apk.py <apk> [...]")
        return 2

    failed = False
    for apk in argv[1:]:
        print(f"\n===== 内容抽查 {apk} =====")
        try:
            missing = check_apk(apk)
        except (zipfile.BadZipFile, tarfile.TarError, OSError) as exc:
            print(f"::error::无法读取 {apk}: {exc}")
            failed = True
            continue

        if missing:
            print(f"::error::{apk} 缺少关键内容，装上会闪退: " + ", ".join(missing))
            failed = True
        else:
            print(f"  => {apk} 检查通过")

    if failed:
        return 1
    print("\n全部 APK 检查通过")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
