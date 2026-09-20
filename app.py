"""宋宝宝的记录 · 移动端 Web App（Bootstrap 风格 + Chart.js 曲线图）

启动：python app.py           （局域网访问，手机浏览器打开提示的地址）
      python app.py --seed    （写入 30 天演示数据）
"""
from __future__ import annotations

import argparse
import csv
import io
import os
import socket

from flask import (Flask, Response, flash, jsonify, redirect, render_template,
                   request, url_for)

import storage
from utils import classify, now_time_str, parse_int, today_str

app = Flask(__name__)
app.secret_key = os.environ.get("BP_SECRET", "blood-pressure-record")
app.jinja_env.globals.update(classify=classify)

RANGES = [("7", "7 天"), ("30", "30 天"), ("90", "90 天"), ("0", "全部")]


@app.context_processor
def inject_common():
    return {"today": today_str(), "ranges": RANGES}


def _days_from_args() -> int | None:
    """URL 参数 days：0 或 None 表示全部数据。"""
    raw = request.args.get("days", "30")
    days = parse_int(raw, 30) or 0
    return days if days > 0 else None


def _validate(form) -> tuple[dict | None, str | None]:
    """校验表单，返回 (数据, 错误信息)。"""
    date = (form.get("date") or "").strip()
    time = (form.get("time") or "").strip()
    systolic = parse_int(form.get("systolic"))
    diastolic = parse_int(form.get("diastolic"))
    pulse = parse_int(form.get("pulse"))
    note = (form.get("note") or "").strip()

    if not date:
        return None, "请选择测量日期"
    if systolic is None or diastolic is None:
        return None, "请填写收缩压与舒张压"
    if not (60 <= systolic <= 300) or not (30 <= diastolic <= 200):
        return None, "血压数值超出合理范围（收缩压 60-300，舒张压 30-200）"
    if diastolic >= systolic:
        return None, "舒张压应小于收缩压"
    if pulse is not None and pulse > 0 and not (30 <= pulse <= 250):
        return None, "心率应在 30-250 之间"

    return {"date": date, "time": time, "systolic": systolic, "diastolic": diastolic,
            "pulse": pulse or None, "note": note}, None


def _read_form(record=None) -> dict:
    """渲染表单用的初始值。"""
    if record:
        return dict(record)
    return {"date": today_str(), "time": now_time_str(), "systolic": "", "diastolic": "",
            "pulse": "", "note": ""}


@app.route("/")
def index():
    days = _days_from_args()
    rows = storage.list_for_chart(days)
    stat = storage.stats(days)
    latest = storage.latest()
    return render_template(
        "index.html",
        days=days or 0,
        ranges=RANGES,
        rows=rows,
        stat=stat,
        latest=latest,
        recent=storage.recent(5),
        chart_rows=[{k: r[k] for k in ("date", "time", "systolic", "diastolic", "pulse")} for r in rows],
    )


@app.route("/api/chart")
def api_chart():
    days = _days_from_args()
    rows = storage.list_for_chart(days)
    return jsonify([
        {"date": r["date"], "time": r["time"], "systolic": r["systolic"],
         "diastolic": r["diastolic"], "pulse": r["pulse"]}
        for r in rows
    ])


@app.route("/add", methods=["GET", "POST"])
def add():
    if request.method == "POST":
        data, error = _validate(request.form)
        if error:
            flash(error, "danger")
            return render_template("form.html", title="添加记录", action=url_for("add"),
                                   record=_read_form(request.form), ranges=RANGES), 400
        storage.add_record(**data)
        flash("记录已保存", "success")
        return redirect(url_for("index"))
    return render_template("form.html", title="添加记录", action=url_for("add"),
                           record=_read_form(), ranges=RANGES)


@app.route("/edit/<int:record_id>", methods=["GET", "POST"])
def edit(record_id: int):
    record = storage.get_record(record_id)
    if record is None:
        flash("记录不存在", "warning")
        return redirect(url_for("records"))
    if request.method == "POST":
        data, error = _validate(request.form)
        if error:
            flash(error, "danger")
            return render_template("form.html", title="编辑记录",
                                   action=url_for("edit", record_id=record_id),
                                   record=_read_form(request.form), ranges=RANGES), 400
        storage.update_record(record_id, **data)
        flash("记录已更新", "success")
        return redirect(url_for("records"))
    return render_template("form.html", title="编辑记录",
                           action=url_for("edit", record_id=record_id),
                           record=_read_form(record), ranges=RANGES)


@app.route("/delete/<int:record_id>", methods=["POST"])
def delete(record_id: int):
    storage.delete_record(record_id)
    flash("记录已删除", "info")
    return redirect(request.referrer or url_for("records"))


@app.route("/records")
def records():
    page = max(parse_int(request.args.get("page"), 1) or 1, 1)
    date_from = request.args.get("from", "")
    date_to = request.args.get("to", "")
    rows, total = storage.list_page(date_from, date_to, page)
    pages = max((total + 19) // 20, 1)
    return render_template("records.html", rows=rows, total=total, page=page, pages=pages,
                           date_from=date_from, date_to=date_to, ranges=RANGES)


@app.route("/export.csv")
def export_csv():
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["日期", "时间", "收缩压(mmHg)", "舒张压(mmHg)", "心率(次/分)", "分级", "备注"])
    for r in storage.all_records():
        writer.writerow([r["date"], r["time"], r["systolic"], r["diastolic"],
                         r["pulse"] or "", classify(r["systolic"], r["diastolic"])["name"], r["note"]])
    output.seek(0)
    return Response(
        output.getvalue().encode("utf-8-sig"),
        mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=blood-pressure.csv"},
    )


@app.route("/manifest.json")
def manifest():
    return app.send_static_file("manifest.json")


@app.route("/sw.js")
def service_worker():
    return app.send_static_file("sw.js")


def lan_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except OSError:
        return "127.0.0.1"


def main():
    parser = argparse.ArgumentParser(description="宋宝宝的记录 App")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=5000)
    parser.add_argument("--seed", action="store_true", help="写入演示数据")
    args = parser.parse_args()

    storage.init_db()
    if args.seed:
        n = storage.seed_demo()
        print(f"已写入 {n} 条演示数据")

    print(f"本机访问：http://127.0.0.1:{args.port}")
    print(f"手机（同一 WiFi）访问：http://{lan_ip()}:{args.port}")
    app.run(host=args.host, port=args.port, debug=False)


if __name__ == "__main__":
    main()
