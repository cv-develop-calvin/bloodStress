"""SQLite 数据访问层：血压 / 用药 / 血常规 的增删改查与统计。"""
from __future__ import annotations

import os
import sqlite3
from datetime import datetime

from lab import CBC_ITEMS, ITEM_ORDER
from models import DDL, INDEXES
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
        for ddl in DDL:
            conn.execute(ddl)
        for idx in INDEXES:
            conn.execute(idx)
        _migrate(conn)


def _migrate(conn: sqlite3.Connection) -> None:
    """为早期版本的 lab_reports 补充新增指标列。"""
    cols = {r["name"] for r in conn.execute("PRAGMA table_info(lab_reports)")}
    for c in ITEM_ORDER:
        if c not in cols:
            conn.execute(f"ALTER TABLE lab_reports ADD COLUMN {c} REAL")


# ---------------------------------------------------------------- 血压
def add_record(date: str, time: str, systolic: int, diastolic: int, pulse, note: str) -> int:
    with get_conn() as conn:
        cur = conn.execute(
            "INSERT INTO records (date, time, systolic, diastolic, pulse, note, created_at)"
            " VALUES (?,?,?,?,?,?,?)",
            (date, time, systolic, diastolic, pulse, note, _now()),
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
    """按日期升序返回指定天数内的血压记录，用于绘制曲线。"""
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


# ---------------------------------------------------------------- 用药
def list_meds(active_only: bool = False):
    sql = "SELECT * FROM meds"
    if active_only:
        sql += " WHERE active=1"
    sql += " ORDER BY active DESC, start_date DESC, id DESC"
    with get_conn() as conn:
        return conn.execute(sql).fetchall()


def get_med(med_id: int):
    with get_conn() as conn:
        return conn.execute("SELECT * FROM meds WHERE id=?", (med_id,)).fetchone()


def add_med(name: str, dosage: str, unit: str, freq: str, times: str,
            start_date: str, end_date: str, note: str, active: int = 1) -> int:
    with get_conn() as conn:
        cur = conn.execute(
            "INSERT INTO meds (name, dosage, unit, freq, times, start_date, end_date, note, active, created_at)"
            " VALUES (?,?,?,?,?,?,?,?,?,?)",
            (name, dosage, unit, freq, times, start_date, end_date, note, active, _now()),
        )
        return cur.lastrowid


def update_med(med_id: int, name: str, dosage: str, unit: str, freq: str, times: str,
               start_date: str, end_date: str, note: str, active: int = 1) -> None:
    with get_conn() as conn:
        conn.execute(
            "UPDATE meds SET name=?, dosage=?, unit=?, freq=?, times=?, start_date=?, end_date=?,"
            " note=?, active=? WHERE id=?",
            (name, dosage, unit, freq, times, start_date, end_date, note, active, med_id),
        )


def delete_med(med_id: int) -> None:
    with get_conn() as conn:
        conn.execute("DELETE FROM med_logs WHERE med_id=?", (med_id,))
        conn.execute("DELETE FROM meds WHERE id=?", (med_id,))


def toggle_med(med_id: int, active: int) -> None:
    with get_conn() as conn:
        conn.execute("UPDATE meds SET active=? WHERE id=?", (active, med_id))


def add_med_log(med_id: int, date: str, time: str, status: str = "taken", note: str = "") -> int:
    with get_conn() as conn:
        cur = conn.execute(
            "INSERT INTO med_logs (med_id, date, time, status, note, created_at) VALUES (?,?,?,?,?,?)",
            (med_id, date, time, status, note, _now()),
        )
        return cur.lastrowid


def delete_med_log(log_id: int) -> None:
    with get_conn() as conn:
        conn.execute("DELETE FROM med_logs WHERE id=?", (log_id,))


def list_med_logs(limit: int = 30, date_from: str = ""):
    where, args = [], []
    if date_from:
        where.append("l.date >= ?")
        args.append(date_from)
    clause = (" WHERE " + " AND ".join(where)) if where else ""
    sql = ("SELECT l.*, m.name AS med_name, m.dosage AS med_dosage, m.unit AS med_unit"
           " FROM med_logs l LEFT JOIN meds m ON m.id = l.med_id" + clause +
           " ORDER BY l.date DESC, l.time DESC, l.id DESC LIMIT ?")
    with get_conn() as conn:
        return conn.execute(sql, (*args, limit)).fetchall()


def taken_map(date: str) -> dict:
    """某天已打卡的：{(med_id, 计划时间): log_id}。"""
    with get_conn() as conn:
        rows = conn.execute("SELECT id, med_id, time FROM med_logs WHERE date=?", (date,)).fetchall()
    return {(r["med_id"], r["time"]): r["id"] for r in rows}


def med_adherence(days: int = 7) -> dict:
    """近 N 天服药依从性：应服次数 / 已服次数。"""
    from datetime import date, timedelta

    meds = [m for m in list_meds(active_only=True) if m["times"]]
    start = date.today() - timedelta(days=days - 1)
    with get_conn() as conn:
        rows = conn.execute(
            "SELECT med_id, date, time FROM med_logs WHERE date >= ?", (start.isoformat(),)
        ).fetchall()
    done = {(r["med_id"], r["date"], r["time"]) for r in rows}

    expected = 0
    taken = 0
    today = date.today()
    for m in meds:
        slots = [t for t in (m["times"] or "").split(",") if t]
        for i in range(days):
            d = (start + timedelta(days=i)).isoformat()
            if m["start_date"] and d < m["start_date"]:
                continue
            if m["end_date"] and d > m["end_date"]:
                continue
            if d > today.isoformat():
                continue
            expected += len(slots)
            taken += sum(1 for s in slots if (m["id"], d, s) in done)
    return {"expected": expected, "taken": taken,
            "rate": round(taken / expected * 100) if expected else None}


def pending_meds(now: datetime | None = None):
    """当前时刻应服但还没打卡的药（用于提醒）。"""
    from datetime import date

    now = now or datetime.now()
    today = date.today().isoformat()
    hhmm = now.strftime("%H:%M")
    done = taken_map(today)
    out = []
    for m in list_meds(active_only=True):
        if m["start_date"] and today < m["start_date"]:
            continue
        if m["end_date"] and today > m["end_date"]:
            continue
        for slot in [t for t in (m["times"] or "").split(",") if t]:
            if slot <= hhmm and (m["id"], slot) not in done:
                out.append({"med": m, "slot": slot,
                            "delay": _minutes_between(slot, hhmm)})
    out.sort(key=lambda x: x["delay"])
    return out


def _minutes_between(hhmm_a: str, hhmm_b: str) -> int:
    def mins(t):
        try:
            h, m = t.split(":")
            return int(h) * 60 + int(m)
        except (ValueError, AttributeError):
            return 0
    return mins(hhmm_b) - mins(hhmm_a)


# ---------------------------------------------------------------- 血常规
def add_lab(date: str, time: str, hospital: str, source: str, photo: str,
            raw_text: str, note: str, values: dict) -> int:
    cols = ["date", "time", "hospital", "source", "photo", "raw_text", "note", "created_at"]
    vals = [date, time, hospital, source, photo, raw_text, note, _now()]
    for c in ITEM_ORDER:
        v = values.get(c)
        cols.append(c)
        vals.append(v)
    sql = f"INSERT INTO lab_reports ({','.join(cols)}) VALUES ({','.join('?' * len(cols))})"
    with get_conn() as conn:
        cur = conn.execute(sql, vals)
        return cur.lastrowid


def update_lab(report_id: int, date: str, time: str, hospital: str, note: str, values: dict) -> None:
    sets = ["date=?", "time=?", "hospital=?", "note=?"]
    vals = [date, time, hospital, note]
    for c in ITEM_ORDER:
        sets.append(f"{c}=?")
        vals.append(values.get(c))
    vals.append(report_id)
    with get_conn() as conn:
        conn.execute(f"UPDATE lab_reports SET {', '.join(sets)} WHERE id=?", vals)


def delete_lab(report_id: int) -> None:
    with get_conn() as conn:
        conn.execute("DELETE FROM lab_reports WHERE id=?", (report_id,))


def get_lab(report_id: int):
    with get_conn() as conn:
        return conn.execute("SELECT * FROM lab_reports WHERE id=?", (report_id,)).fetchone()


def list_lab(limit: int = 50, date_from: str = ""):
    where, args = [], []
    if date_from:
        where.append("date >= ?")
        args.append(date_from)
    clause = (" WHERE " + " AND ".join(where)) if where else ""
    with get_conn() as conn:
        return conn.execute(
            "SELECT * FROM lab_reports" + clause + " ORDER BY date DESC, id DESC LIMIT ?",
            (*args, limit),
        ).fetchall()


def latest_lab():
    with get_conn() as conn:
        return conn.execute("SELECT * FROM lab_reports ORDER BY date DESC, id DESC LIMIT 1").fetchone()


def lab_series(item: str, days: int | None = None):
    """某指标的曲线数据 [(date, value)]（升序）。"""
    if item not in CBC_ITEMS:
        return []
    start = start_date_of(days)
    sql = f"SELECT date, {item} AS v FROM lab_reports WHERE {item} IS NOT NULL"
    args: tuple = ()
    if start:
        sql += " AND date >= ?"
        args = (start,)
    sql += " ORDER BY date ASC, id ASC"
    with get_conn() as conn:
        return [(r["date"], r["v"]) for r in conn.execute(sql, args).fetchall()]


def _now() -> str:
    return datetime.now().isoformat(timespec="seconds")


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
                (d, "08:00", sys_v, dia_v, random.randint(62, 88), "", _now()),
            )
    return days
