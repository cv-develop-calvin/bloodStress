"""血常规（CBC）项目定义、正常区间判定、以及拍照识别（OCR）后的文本解析。

不依赖任何外部服务：OCR 在手机端由 tesseract.js 完成（首次需联网下载模型），
若离线则退化为「手动输入」。
"""
from __future__ import annotations

import re

# 常用血常规项目：key -> 定义
#   label 显示名 / unit 单位 / low,high 参考区间 / aliases OCR 可能识别出的写法
CBC_ITEMS: "dict[str, dict]" = {
    "wbc": {
        "label": "白细胞", "unit": "10⁹/L", "low": 4.0, "high": 10.0,
        "aliases": ["WBC", "白细胞", "白細胞", "白细胞计数", "WBC-BF"],
    },
    "rbc": {
        "label": "红细胞", "unit": "10¹²/L", "low": 3.8, "high": 5.8,
        "aliases": ["RBC", "红细胞", "红细胞计数"],
    },
    "hgb": {
        "label": "血红蛋白", "unit": "g/L", "low": 115, "high": 175,
        "aliases": ["HGB", "HB", "血红蛋白", "血红蛋白浓度"],
    },
    "hct": {
        "label": "红细胞压积", "unit": "%", "low": 35.0, "high": 50.0,
        "aliases": ["HCT", "红细胞压积", "红细胞比容"],
    },
    "mcv": {
        "label": "平均红细胞体积", "unit": "fL", "low": 82.0, "high": 100.0,
        "aliases": ["MCV", "平均红细胞体积"],
    },
    "mch": {
        "label": "平均血红蛋白量", "unit": "pg", "low": 27.0, "high": 34.0,
        "aliases": ["MCH", "平均血红蛋白量", "平均血红蛋白含量"],
    },
    "mchc": {
        "label": "平均血红蛋白浓度", "unit": "g/L", "low": 316, "high": 354,
        "aliases": ["MCHC", "平均血红蛋白浓度"],
    },
    "plt": {
        "label": "血小板", "unit": "10⁹/L", "low": 125, "high": 350,
        "aliases": ["PLT", "血小板", "血小板计数"],
    },
    "lym_pct": {
        "label": "淋巴细胞百分比", "unit": "%", "low": 20.0, "high": 50.0,
        "aliases": ["LYM%", "LYMPH%", "淋巴细胞比率", "淋巴细胞百分比"],
    },
    "neut_pct": {
        "label": "中性粒细胞百分比", "unit": "%", "low": 40.0, "high": 75.0,
        "aliases": ["NEUT%", "NEU%", "中性粒细胞比率", "中性粒细胞百分比"],
    },
    "mono_pct": {
        "label": "单核细胞百分比", "unit": "%", "low": 3.0, "high": 10.0,
        "aliases": ["MONO%", "单核细胞比率", "单核细胞百分比"],
    },
    "eos_pct": {
        "label": "嗜酸性粒细胞百分比", "unit": "%", "low": 0.4, "high": 8.0,
        "aliases": ["EO%", "EOS%", "嗜酸性粒细胞比率"],
    },
    "crp": {
        "label": "超敏C反应蛋白", "unit": "mg/L", "low": 0.0, "high": 3.0,
        "aliases": ["hs-CRP", "CRP", "超敏C反应蛋白", "C反应蛋白"],
    },
}

# 表单顺序（首页/表单按此顺序渲染）
ITEM_ORDER = list(CBC_ITEMS.keys())

ABNORMAL = {
    "low": {"name": "偏低", "css": "text-bg-warning"},
    "high": {"name": "偏高", "css": "text-bg-danger"},
    "normal": {"name": "正常", "css": "text-bg-success"},
}


def judge(key: str, value: float | None) -> dict:
    """判断单项是否在参考区间内。"""
    if value is None:
        return {"key": "none", "name": "—", "css": "text-bg-light text-secondary"}
    item = CBC_ITEMS[key]
    if value < item["low"]:
        return {"key": "low", **ABNORMAL["low"]}
    if value > item["high"]:
        return {"key": "high", **ABNORMAL["high"]}
    return {"key": "normal", **ABNORMAL["normal"]}


def abnormal_count(values: "dict[str, float|None]") -> int:
    return sum(1 for k, v in values.items() if k in CBC_ITEMS and judge(k, v)["key"] in ("low", "high"))


def _aliases_map() -> "dict[str, str]":
    """别名（大写、去空格）-> 项目 key。长别名优先匹配。"""
    out: "dict[str, str]" = {}
    for key, item in CBC_ITEMS.items():
        for alias in item["aliases"]:
            out[alias.upper().replace(" ", "")] = key
    return out


ALIASES = _aliases_map()

NUM = r"[-+]?\d+(?:[.,]\d+)?"

# 一行形如： "WBC 白细胞 12.34 ↑ 3.5-9.5" 或 "HGB 血红蛋白 98 g/L"
LINE_RE = re.compile(rf"^(?P<name>[^0-9]{{1,20}}?)\s*[:：]?\s*(?P<value>{NUM})(?P<rest>.*)$")


def _match_key(text: str) -> str | None:
    t = text.upper().replace(" ", "").replace("：", ":")
    if t in ALIASES:
        return ALIASES[t]
    # 允许 OCR 把百分号识别成 8 / %，做一次宽松匹配（取最长别名为准）
    for alias in sorted(ALIASES, key=len, reverse=True):
        if len(alias) >= 3 and alias in t:
            return ALIASES[alias]
    return None


def parse_ocr_text(text: str) -> "dict[str, float]":
    """把 OCR 出来的报告文本解析成 {项目key: 数值}，解析不出的项目不返回。"""
    result: "dict[str, float]" = {}
    for raw_line in (text or "").splitlines():
        line = raw_line.strip()
        if not line:
            continue
        m = LINE_RE.match(line)
        if not m:
            continue
        name = m.group("name")
        key = _match_key(name)
        if not key:
            continue
        try:
            value = float(m.group("value").replace(",", "."))
        except ValueError:
            continue
        # 参考区间里出现的数字不应被当作测量值：若 rest 以 “-”/“~” 开头说明取到了区间起点
        rest = m.group("rest").strip()
        if rest.startswith(("-", "~", "—")):
            continue
        result.setdefault(key, value)
    return result
