"""笔记照片的存储：保存原图 + 生成缩略图，统一放在数据目录下的 photos/。

Android 上数据目录指向应用私有目录（ANDROID_PRIVATE），
桌面下则落在项目内的 data/ 目录，两者都不需要额外权限。
"""
from __future__ import annotations

import os
import shutil
import uuid
from datetime import datetime

import storage

# Pillow 惰性导入：万一打包时没带上 pillow，也只影响照片压缩功能，
# 不会因为顶层 ImportError 让整个应用启动失败。
try:
    from PIL import Image, ImageOps
    HAS_PIL = True
except ImportError:  # pragma: no cover
    Image = ImageOps = None
    HAS_PIL = False

ALLOWED = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".heic", ".heif"}
MAX_SIDE = 1600          # 原图最长边，超过则等比缩小，省空间
THUMB_SIDE = 320         # 缩略图最长边


def photos_dir() -> str:
    path = os.path.join(storage._data_dir(), "photos")
    os.makedirs(path, exist_ok=True)
    return path


def photo_path(filename: str) -> str:
    return os.path.join(photos_dir(), filename)


def ext_ok(filename: str) -> bool:
    return os.path.splitext(filename or "")[1].lower() in ALLOWED


def save_upload(file_storage) -> dict | None:
    """保存上传的图片，返回 {filename, thumb, size}；格式不支持时返回 None。"""
    name = file_storage.filename or ""
    ext = os.path.splitext(name)[1].lower()
    if ext not in ALLOWED:
        return None

    stamp = datetime.now().strftime("%Y%m%d%H%M%S")
    base = f"{stamp}_{uuid.uuid4().hex[:8]}"
    filename = base + ".jpg"
    thumb_name = base + "_thumb.jpg"
    target = photo_path(filename)
    thumb_path = photo_path(thumb_name)

    # 没有 Pillow 时退化为直接保存原文件，不再压缩
    if not HAS_PIL:
        try:
            file_storage.stream.seek(0)
            with open(target, "wb") as f:
                shutil.copyfileobj(file_storage.stream, f)
        except Exception:  # noqa: BLE001
            return None
        try:
            shutil.copyfile(target, thumb_path)
        except OSError:
            thumb_name = filename
        return {"filename": filename, "thumb": thumb_name,
                "size": os.path.getsize(target)}

    try:
        img = Image.open(file_storage.stream)
        img = ImageOps.exif_transpose(img)          # 手机照片按 EXIF 转正
    except Exception:  # noqa: BLE001 不是有效图片
        return None

    # 原图（统一转 JPEG，透明背景铺白底）
    big = img.copy()
    big.thumbnail((MAX_SIDE, MAX_SIDE), Image.LANCZOS)
    if big.mode in ("RGBA", "LA", "P"):
        big = big.convert("RGBA")
        canvas = Image.new("RGB", big.size, (255, 255, 255))
        canvas.paste(big, mask=big.split()[-1])
        big = canvas
    else:
        big = big.convert("RGB")
    big.save(target, "JPEG", quality=86, optimize=True)

    # 缩略图
    small = big.copy()
    small.thumbnail((THUMB_SIDE, THUMB_SIDE), Image.LANCZOS)
    small.save(thumb_path, "JPEG", quality=82, optimize=True)

    return {"filename": filename, "thumb": thumb_name,
            "size": os.path.getsize(target)}


def remove(filename: str) -> None:
    """删除照片文件（找不到就忽略）。"""
    if not filename:
        return
    # 防止路径穿越
    safe = os.path.basename(filename)
    for f in (safe, os.path.splitext(safe)[0] + "_thumb.jpg"):
        p = photo_path(f)
        if os.path.isfile(p):
            try:
                os.remove(p)
            except OSError:
                pass
