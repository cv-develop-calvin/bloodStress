"""血压分级与通用工具函数。"""
from __future__ import annotations

from datetime import date, datetime, timedelta

# 分级标准参考《中国高血压防治指南》：以收缩压/舒张压中较高的等级为准
LEVELS = {
    "low": {"name": "血压偏低", "css": "text-bg-secondary",
            "advice": "如伴头晕乏力请就医，注意补水与缓慢起身。"},
    "normal": {"name": "正常血压", "css": "text-bg-success",
               "advice": "保持规律作息、低盐饮食与适度运动。"},
    "elevated": {"name": "正常高值", "css": "text-bg-info",
                 "advice": "建议限盐、控制体重，定期监测血压。"},
    "stage1": {"name": "1 级高血压", "css": "text-bg-warning",
               "advice": "建议就医评估，改善生活方式并持续监测。"},
    "stage2": {"name": "2 级高血压", "css": "text-bg-danger",
               "advice": "请尽快就诊，遵医嘱服药并记录血压。"},
    "stage3": {"name": "3 级高血压", "css": "text-bg-danger",
               "advice": "血压显著升高，请立即就医或联系医生。"},
}


def classify(systolic: int, diastolic: int) -> dict:
    """根据收缩压 / 舒张压返回分级信息。"""
    sys_level = _sys_level(systolic)
    dia_level = _dia_level(diastolic)
    order = ["low", "normal", "elevated", "stage1", "stage2", "stage3"]
    key = sys_level if order.index(sys_level) >= order.index(dia_level) else dia_level
    info = LEVELS[key]
    return {"key": key, "name": info["name"], "css": info["css"], "advice": info["advice"]}


def _sys_level(systolic: int) -> str:
    if systolic < 90:
        return "low"
    if systolic < 120:
        return "normal"
    if systolic < 140:
        return "elevated"
    if systolic < 160:
        return "stage1"
    if systolic < 180:
        return "stage2"
    return "stage3"


def _dia_level(diastolic: int) -> str:
    if diastolic < 60:
        return "low"
    if diastolic < 80:
        return "normal"
    if diastolic < 90:
        return "elevated"
    if diastolic < 100:
        return "stage1"
    if diastolic < 110:
        return "stage2"
    return "stage3"


def today_str() -> str:
    return date.today().isoformat()


def now_time_str() -> str:
    return datetime.now().strftime("%H:%M")


def start_date_of(days: int | None) -> str | None:
    """days 为 None / 0 时返回 None（表示全部数据）。"""
    if not days:
        return None
    return (date.today() - timedelta(days=days - 1)).isoformat()


def parse_int(value, default=None):
    try:
        return int(str(value).strip())
    except (TypeError, ValueError):
        return default
