"""笔记照片的存储：保存原图 + 生成缩略图，统一放在数据目录下的 photos/。

Android 上数据目录指向应用私有目录（ANDROID_PRIVATE），
桌面下则落在项目内的 data/ 目录，两者都不需要额外权限。
"""
from __future__ import annotations

import os
import uuid
from datetime import datetime

from PIL import Image, ImageOps

import storage

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
