"""数据表结构定义（用药、用药提醒、血常规）。"""
from __future__ import annotations

from lab import CBC_ITEMS, ITEM_ORDER

CBC_COLUMNS = ITEM_ORDER

DDL = [
    # 用药记录
    """
    CREATE TABLE IF NOT EXISTS meds (
        id         INTEGER PRIMARY KEY AUTOINCREMENT,
        name       TEXT NOT NULL,
        dosage     TEXT NOT NULL DEFAULT '',
        unit       TEXT NOT NULL DEFAULT '',
        freq       TEXT NOT NULL DEFAULT '每日 1 次',
        times      TEXT NOT NULL DEFAULT '',
        start_date TEXT NOT NULL DEFAULT '',
        end_date   TEXT NOT NULL DEFAULT '',
        note       TEXT NOT NULL DEFAULT '',
        active     INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL
    )
    """,
    # 服药打卡记录
    """
    CREATE TABLE IF NOT EXISTS med_logs (
        id       INTEGER PRIMARY KEY AUTOINCREMENT,
        med_id   INTEGER NOT NULL,
        date     TEXT NOT NULL,
        time     TEXT NOT NULL DEFAULT '',
        status   TEXT NOT NULL DEFAULT 'taken',
        note     TEXT NOT NULL DEFAULT '',
        created_at TEXT NOT NULL,
        FOREIGN KEY (med_id) REFERENCES meds(id) ON DELETE CASCADE
    )
    """,
    # 血常规记录（宽表：每个项目一列）
    f"""
    CREATE TABLE IF NOT EXISTS lab_reports (
        id         INTEGER PRIMARY KEY AUTOINCREMENT,
        date       TEXT NOT NULL,
        time       TEXT NOT NULL DEFAULT '',
        hospital   TEXT NOT NULL DEFAULT '',
        source     TEXT NOT NULL DEFAULT 'manual',
        photo      TEXT NOT NULL DEFAULT '',
        raw_text   TEXT NOT NULL DEFAULT '',
        note       TEXT NOT NULL DEFAULT '',
        created_at TEXT NOT NULL,
        {', '.join(f'{c} REAL' for c in CBC_COLUMNS)}
    )
    """,
]

INDEXES = [
    "CREATE INDEX IF NOT EXISTS idx_med_logs_date ON med_logs(date)",
    "CREATE INDEX IF NOT EXISTS idx_lab_date ON lab_reports(date)",
]


def lab_defaults() -> dict:
    return {c: None for c in CBC_COLUMNS}


def lab_items():
    return CBC_ITEMS
