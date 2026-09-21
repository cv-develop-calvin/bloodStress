"""宋宝宝的记录 · 移动端 Web App（Bootstrap 风格 + Chart.js 曲线图）

功能：血压记录与趋势、用药记录与提醒、血常规记录与拍照识别。
启动：python app.py           （局域网访问，手机浏览器打开提示的地址）
      python app.py --seed    （写入 30 天演示数据）
"""
from __future__ import annotations

import argparse
import csv
import io
import os
import socket
from datetime import datetime

from flask import (Flask, Response, flash, jsonify, redirect, render_template,
                   request, url_for)

import storage
from lab import CBC_ITEMS, ITEM_ORDER, abnormal_count, judge, parse_ocr_text
from utils import classify, now_time_str, parse_int, today_str

app = Flask(__name__)
app.secret_key = os.environ.get("BP_SECRET", "blood-pressure-record")
app.jinja_env.globals.update(classify=classify, judge=judge, CBC_ITEMS=CBC_ITEMS,
                             ITEM_ORDER=ITEM_ORDER, abnormal_count=abnormal_count)

RANGES = [("7", "7 天"), ("30", "30 天"), ("90", "90 天"), ("0", "全部")]
FREQS = ["每日 1 次", "每日 2 次", "每日 3 次", "隔日 1 次", "每周 1 次", "按需服用"]


@app.context_processor
def inject_common():
    return {"today": today_str(), "ranges": RANGES, "freqs": FREQS, "now_str": now_time_str()}


def _days_from_args() -> int | None:
    """URL 参数 days：0 或 None 表示全部数据。"""
    raw = request.args.get("days", "30")
    days = parse_int(raw, 30) or 0
    return days if days > 0 else None


def _parse_times(raw: str) -> str:
    """把 "08:00,20:00" 规范化（去空、排序、去重）。"""
    items = [t.strip() for t in (raw or "").replace("；", ",").replace(";", ",").split(",")]
    items = sorted({t for t in items if t})
    return ",".join(items)


# ================================================================ 血压
def _validate(form) -> tuple[dict | None, str | None]:
    """校验血压表单，返回 (数据, 错误信息)。"""
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
    """渲染血压表单用的初始值。"""
    if record:
        return dict(record)
    return {"date": today_str(), "time": now_time_str(), "systolic": "", "diastolic": "",
            "pulse": "", "note": ""}


@app.route("/")
def index():
    days = _days_from_args()
    rows = storage.list_for_chart(days)
    today = today_str()
    return render_template(
        "index.html",
        days=days or 0,
        rows=rows,
        stat=storage.stats(days),
        latest=storage.latest(),
        recent=storage.recent(5),
        chart_rows=[{k: r[k] for k in ("date", "time", "systolic", "diastolic", "pulse")} for r in rows],
        pending=storage.pending_meds(datetime.now()),
        adherence=storage.med_adherence(7),
        latest_lab=storage.latest_lab(),
        today_logs=storage.list_med_logs(limit=5, date_from=today),
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
            return render_template("form.html", title="添加血压记录", action=url_for("add"),
                                   record=_read_form(request.form)), 400
        storage.add_record(**data)
        flash("血压记录已保存", "success")
        return redirect(url_for("index"))
    return render_template("form.html", title="添加血压记录", action=url_for("add"),
                           record=_read_form())


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
            return render_template("form.html", title="编辑血压记录",
                                   action=url_for("edit", record_id=record_id),
                                   record=_read_form(request.form)), 400
        storage.update_record(record_id, **data)
        flash("记录已更新", "success")
        return redirect(url_for("records"))
    return render_template("form.html", title="编辑血压记录",
                           action=url_for("edit", record_id=record_id),
                           record=_read_form(record))


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
                           date_from=date_from, date_to=date_to)


# ================================================================ 用药
def _read_med_form(form=None, row=None) -> dict:
    if form is not None:
        return {
            "name": (form.get("name") or "").strip(),
            "dosage": (form.get("dosage") or "").strip(),
            "unit": (form.get("unit") or "").strip() or "mg",
            "freq": (form.get("freq") or "").strip() or FREQS[0],
            "times": (form.get("times") or "").strip(),
            "start_date": (form.get("start_date") or "").strip() or today_str(),
            "end_date": (form.get("end_date") or "").strip(),
            "note": (form.get("note") or "").strip(),
            "active": 1 if form.get("active") else 0,
        }
    if row is not None:
        return {k: row[k] for k in ("name", "dosage", "unit", "freq", "times",
                                    "start_date", "end_date", "note", "active")}
    return {"name": "", "dosage": "", "unit": "mg", "freq": FREQS[0], "times": "08:00",
            "start_date": today_str(), "end_date": "", "note": "", "active": 1}


def _validate_med(form) -> tuple[dict | None, str | None]:
    data = _read_med_form(form)
    if not data["name"]:
        return None, "请填写药品名称"
    data["times"] = _parse_times(data["times"])
    if data["freq"] != "按需服用" and not data["times"]:
        return None, "请至少设置一个服药时间"
    for t in data["times"].split(","):
        if t and not (len(t) == 5 and t[2] == ":"):
            return None, f"时间格式不正确：{t}"
    if data["end_date"] and data["end_date"] < data["start_date"]:
        return None, "结束日期不能早于开始日期"
    return data, None


@app.route("/meds")
def meds():
    today = today_str()
    return render_template("meds.html", meds=storage.list_meds(),
                           adherence=storage.med_adherence(7),
                           logs=storage.list_med_logs(limit=20),
                           pending=storage.pending_meds(datetime.now()),
                           taken=storage.taken_map(today),
                           today=today)


@app.route("/meds/add", methods=["GET", "POST"])
def meds_add():
    if request.method == "POST":
        data, error = _validate_med(request.form)
        if error:
            flash(error, "danger")
            return render_template("med_form.html", title="添加药品",
                                   action=url_for("meds_add"),
                                   med=_read_med_form(request.form)), 400
        storage.add_med(**data)
        flash("药品已添加，到时间会提醒你", "success")
        return redirect(url_for("meds"))
    return render_template("med_form.html", title="添加药品",
                           action=url_for("meds_add"), med=_read_med_form())


@app.route("/meds/edit/<int:med_id>", methods=["GET", "POST"])
def meds_edit(med_id: int):
    row = storage.get_med(med_id)
    if row is None:
        flash("药品不存在", "warning")
        return redirect(url_for("meds"))
    if request.method == "POST":
        data, error = _validate_med(request.form)
        if error:
            flash(error, "danger")
            return render_template("med_form.html", title="编辑药品",
                                   action=url_for("meds_edit", med_id=med_id),
                                   med=_read_med_form(request.form)), 400
        storage.update_med(med_id, **data)
        flash("药品已更新", "success")
        return redirect(url_for("meds"))
    return render_template("med_form.html", title="编辑药品",
                           action=url_for("meds_edit", med_id=med_id),
                           med=_read_med_form(row=row))


@app.route("/meds/delete/<int:med_id>", methods=["POST"])
def meds_delete(med_id: int):
    storage.delete_med(med_id)
    flash("药品及其服药记录已删除", "info")
    return redirect(url_for("meds"))


@app.route("/meds/toggle/<int:med_id>", methods=["POST"])
def meds_toggle(med_id: int):
    row = storage.get_med(med_id)
    if row is None:
        flash("药品不存在", "warning")
        return redirect(url_for("meds"))
    storage.toggle_med(med_id, 0 if row["active"] else 1)
    flash("已停用" if row["active"] else "已重新启用", "info")
    return redirect(url_for("meds"))


@app.route("/meds/take", methods=["POST"])
def meds_take():
    """服药打卡；若当天该时段已有记录则取消打卡。"""
    med_id = parse_int(request.form.get("med_id"))
    slot = (request.form.get("time") or now_time_str()).strip()
    date = (request.form.get("date") or today_str()).strip()
    if not med_id:
        flash("参数不正确", "danger")
        return redirect(request.referrer or url_for("meds"))
    done = storage.taken_map(date)
    log_id = done.get((med_id, slot))
    if log_id:
        storage.delete_med_log(log_id)
        flash("已撤销打卡", "info")
    else:
        storage.add_med_log(med_id, date, slot)
        flash("已记录服药", "success")
    return redirect(request.referrer or url_for("meds"))


@app.route("/api/pending")
def api_pending():
    """给前端轮询用：返回当前到点未服的药。"""
    items = storage.pending_meds(datetime.now())
    return jsonify([
        {"med_id": p["med"]["id"], "name": p["med"]["name"],
         "dosage": p["med"]["dosage"], "unit": p["med"]["unit"],
         "slot": p["slot"], "delay": p["delay"]}
        for p in items
    ])


# ================================================================ 血常规
def _read_lab_values(form) -> dict:
    values = {}
    for key in ITEM_ORDER:
        values[key] = _to_float(form.get(key))
    return values


def _to_float(raw):
    if raw is None:
        return None
    s = str(raw).strip().replace(",", ".")
    if not s:
        return None
    try:
        return float(s)
    except ValueError:
        return None


def _read_lab_form(form=None, row=None) -> dict:
    if form is not None:
        return {"date": (form.get("date") or "").strip() or today_str(),
                "time": (form.get("time") or "").strip(),
                "hospital": (form.get("hospital") or "").strip(),
                "note": (form.get("note") or "").strip(),
                "source": form.get("source") or "manual",
                "photo": form.get("photo") or "",
                "raw_text": form.get("raw_text") or ""}
    if row is not None:
        return {"date": row["date"], "time": row["time"], "hospital": row["hospital"],
                "note": row["note"], "source": row["source"], "photo": row["photo"],
                "raw_text": row["raw_text"]}
    return {"date": today_str(), "time": now_time_str(), "hospital": "", "note": "",
            "source": "manual", "photo": "", "raw_text": ""}


@app.route("/lab")
def lab():
    days = _days_from_args()
    reports = storage.list_lab(limit=50, date_from="")
    latest = storage.latest_lab()
    items_with_data = [k for k in ITEM_ORDER if storage.lab_series(k, None)]
    return render_template("lab.html", reports=reports, latest=latest,
                           series_items=items_with_data, days=days or 0)


@app.route("/lab/chart/<item>")
def lab_chart(item: str):
    days = _days_from_args()
    if item not in CBC_ITEMS:
        return jsonify({"error": "unknown item"}), 404
    info = CBC_ITEMS[item]
    return jsonify({"item": item, "label": info["label"], "unit": info["unit"],
                    "low": info["low"], "high": info["high"],
                    "points": storage.lab_series(item, days)})


@app.route("/lab/scan")
def lab_scan():
    """拍照/上传报告照片，由前端 OCR 识别后进入确认表单。"""
    return render_template("lab_scan.html", items=ITEM_ORDER)


@app.route("/lab/parse", methods=["POST"])
def lab_parse():
    """接收 OCR 文本，返回解析出的指标（不直接入库，需人工确认）。"""
    text = request.get_json(silent=True) or {}
    raw = text.get("text", "")
    parsed = parse_ocr_text(raw)
    return jsonify({
        "values": parsed,
        "items": [{"key": k, "label": CBC_ITEMS[k]["label"], "unit": CBC_ITEMS[k]["unit"],
                   "low": CBC_ITEMS[k]["low"], "high": CBC_ITEMS[k]["high"]}
                  for k in parsed],
        "matched": len(parsed),
    })


@app.route("/lab/add", methods=["GET", "POST"])
def lab_add():
    if request.method == "POST":
        data = _read_lab_form(request.form)
        values = _read_lab_values(request.form)
        if data["date"] > today_str():
            flash("报告日期不能晚于今天", "danger")
            return render_template("lab_form.html", title="添加血常规", action=url_for("lab_add"),
                                   report=data, values=values), 400
        if not any(v is not None for v in values.values()):
            flash("请至少填写一项血常规指标", "danger")
            return render_template("lab_form.html", title="添加血常规", action=url_for("lab_add"),
                                   report=data, values=values), 400
        storage.add_lab(date=data["date"], time=data["time"], hospital=data["hospital"],
                        source=data["source"], photo=data["photo"], raw_text=data["raw_text"],
                        note=data["note"], values=values)
        flash(f"血常规已保存（{abnormal_count(values)} 项异常）", "success")
        return redirect(url_for("lab"))
    prefill = {}
    for key in ITEM_ORDER:
        prefill[key] = request.args.get(key, "")
    report = _read_lab_form()
    report["source"] = request.args.get("source", "manual")
    report["photo"] = request.args.get("photo", "")
    report["raw_text"] = request.args.get("raw_text", "")
    if request.args.get("hospital"):
        report["hospital"] = request.args["hospital"]
    return render_template("lab_form.html", title="添加血常规", action=url_for("lab_add"),
                           report=report, values=prefill)


@app.route("/lab/edit/<int:report_id>", methods=["GET", "POST"])
def lab_edit(report_id: int):
    row = storage.get_lab(report_id)
    if row is None:
        flash("报告不存在", "warning")
        return redirect(url_for("lab"))
    if request.method == "POST":
        data = _read_lab_form(request.form)
        values = _read_lab_values(request.form)
        storage.update_lab(report_id, data["date"], data["time"], data["hospital"],
                           data["note"], values)
        flash("血常规已更新", "success")
        return redirect(url_for("lab"))
    return render_template("lab_form.html", title="编辑血常规",
                           action=url_for("lab_edit", report_id=report_id),
                           report=_read_lab_form(row=row),
                           values={k: (row[k] if row[k] is not None else "") for k in ITEM_ORDER})


@app.route("/lab/delete/<int:report_id>", methods=["POST"])
def lab_delete(report_id: int):
    storage.delete_lab(report_id)
    flash("报告已删除", "info")
    return redirect(url_for("lab"))


# ================================================================ 导出与静态资源
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
        headers={"Content-Disposition": "attachment; filename=songbaobao-bp.csv"},
    )


@app.route("/export_lab.csv")
def export_lab_csv():
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["日期", "时间", "医院", "来源", "备注"] + [CBC_ITEMS[k]["label"] for k in ITEM_ORDER])
    for r in storage.list_lab(limit=10000):
        writer.writerow([r["date"], r["time"], r["hospital"], r["source"], r["note"]] +
                        [r[k] if r[k] is not None else "" for k in ITEM_ORDER])
    output.seek(0)
    return Response(
        output.getvalue().encode("utf-8-sig"),
        mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=songbaobao-lab.csv"},
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
