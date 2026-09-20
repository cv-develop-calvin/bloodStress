"""SQLite 数据访问层：宋宝宝的记录的增删改查与统计。"""
from __future__ import annotations

import os
import sqlite3
from datetime import datetime

from utils import start_date_of

BASE_DIR = os.path.dirname(os.path.abspath(__file__))


def _data_dir() -> str:
    """数据库存放目录：优先 Android 私有目录，其次环境变量，最后项目目录。"""
    for candidate in (
        os.environ.get("BP_DATA_DIR"),
        os.environ.get("ANDROID_APP_PATH"),
        os.environ.get("ANDROID_PRIVATE"),
    ):
        if candidate:
            return candidate
    return os.path.join(BASE_DIR, "data")


def db_path() -> str:
    return os.path.join(_data_dir(), "bp.db")


def get_conn() -> sqlite3.Connection:
    path = db_path()
    os.makedirs(os.path.dirname(path), exist_ok=True)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with get_conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS records (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                date       TEXT NOT NULL,
                time       TEXT NOT NULL DEFAULT '',
                systolic   INTEGER NOT NULL,
                diastolic  INTEGER NOT NULL,
                pulse      INTEGER,
                note       TEXT NOT NULL DEFAULT '',
                created_at TEXT NOT NULL
            )
            """
        )
        conn.execute("CREATE INDEX IF NOT EXISTS idx_records_date ON records(date)")


def add_record(date: str, time: str, systolic: int, diastolic: int, pulse, note: str) -> int:
    with get_conn() as conn:
        cur = conn.execute(
            "INSERT INTO records (date, time, systolic, diastolic, pulse, note, created_at)"
            " VALUES (?,?,?,?,?,?,?)",
            (date, time, systolic, diastolic, pulse, note, datetime.now().isoformat(timespec="seconds")),
        )
        return cur.lastrowid


def update_record(record_id: int, date: str, time: str, systolic: int, diastolic: int, pulse, note: str) -> None:
    with get_conn() as conn:
        conn.execute(
            "UPDATE records SET date=?, time=?, systolic=?, diastolic=?, pulse=?, note=? WHERE id=?",
            (date, time, systolic, diastolic, pulse, note, record_id),
        )


def delete_record(record_id: int) -> None:
    with get_conn() as conn:
        conn.execute("DELETE FROM records WHERE id=?", (record_id,))


def get_record(record_id: int):
    with get_conn() as conn:
        return conn.execute("SELECT * FROM records WHERE id=?", (record_id,)).fetchone()


def latest():
    with get_conn() as conn:
        return conn.execute(
            "SELECT * FROM records ORDER BY date DESC, time DESC, id DESC LIMIT 1"
        ).fetchone()


def recent(limit: int = 5):
    with get_conn() as conn:
        return conn.execute(
            "SELECT * FROM records ORDER BY date DESC, time DESC, id DESC LIMIT ?", (limit,)
        ).fetchall()


def list_for_chart(days: int | None):
    """按日期升序返回指定天数内的记录，用于绘制曲线。"""
    start = start_date_of(days)
    sql = "SELECT * FROM records"
    args: tuple = ()
    if start:
        sql += " WHERE date >= ?"
        args = (start,)
    sql += " ORDER BY date ASC, time ASC, id ASC"
    with get_conn() as conn:
        return conn.execute(sql, args).fetchall()


def stats(days: int | None) -> dict:
    start = start_date_of(days)
    sql = ("SELECT COUNT(*) AS n, AVG(systolic) AS avg_sys, AVG(diastolic) AS avg_dia,"
           " AVG(pulse) AS avg_pulse, MAX(systolic) AS max_sys, MIN(systolic) AS min_sys,"
           " MAX(diastolic) AS max_dia, MIN(diastolic) AS min_dia FROM records")
    args: tuple = ()
    if start:
        sql += " WHERE date >= ?"
        args = (start,)
    with get_conn() as conn:
        row = conn.execute(sql, args).fetchone()
    return {k: row[k] for k in row.keys()} if row else {}


def list_page(date_from: str = "", date_to: str = "", page: int = 1, per_page: int = 20):
    where, args = [], []
    if date_from:
        where.append("date >= ?")
        args.append(date_from)
    if date_to:
        where.append("date <= ?")
        args.append(date_to)
    clause = (" WHERE " + " AND ".join(where)) if where else ""
    offset = (page - 1) * per_page
    with get_conn() as conn:
        total = conn.execute("SELECT COUNT(*) AS c FROM records" + clause, args).fetchone()["c"]
        rows = conn.execute(
            "SELECT * FROM records" + clause +
            " ORDER BY date DESC, time DESC, id DESC LIMIT ? OFFSET ?",
            (*args, per_page, offset),
        ).fetchall()
    return rows, total


def all_records():
    with get_conn() as conn:
        return conn.execute("SELECT * FROM records ORDER BY date ASC, time ASC").fetchall()


def seed_demo(days: int = 30) -> int:
    """生成演示数据，便于查看曲线效果。"""
    import random
    from datetime import date, timedelta

    random.seed(20240601)
    with get_conn() as conn:
        conn.execute("DELETE FROM records")
        for i in range(days):
            d = (date.today() - timedelta(days=days - 1 - i)).isoformat()
            sys_v = random.randint(115, 148)
            dia_v = random.randint(72, 95)
            conn.execute(
                "INSERT INTO records (date, time, systolic, diastolic, pulse, note, created_at)"
                " VALUES (?,?,?,?,?,?,?)",
                (d, "08:00", sys_v, dia_v, random.randint(62, 88), "",
                 datetime.now().isoformat(timespec="seconds")),
            )
    return days
