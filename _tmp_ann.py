"""临时脚本：按提交等待构建完成并读取编译错误注解（用完即删）。"""
import json
import time
import urllib.request

op = urllib.request.build_opener(urllib.request.ProxyHandler({"https": "http://127.0.0.1:7897"}))
BASE = "https://api.github.com/repos/cv-develop-calvin/bloodStress"
TARGET = "6157477"


def get(u, timeout=40):
    return json.load(op.open(urllib.request.Request(
        u, headers={"User-Agent": "x", "Accept": "application/vnd.github+json"}), timeout=timeout))


run = None
for i in range(24):                       # 最多约 12 分钟
    for r in get(BASE + "/actions/runs?per_page=6")["workflow_runs"]:
        if r["head_sha"].startswith(TARGET):
            run = r
            break
    if run and run["status"] == "completed":
        break
    if run:
        print(f"[{i}] {run['status']}")
    time.sleep(30)

if not run:
    print("未找到该提交的运行")
    raise SystemExit(0)

print("\nrun", run["id"], run["status"], run["conclusion"])
print("\n=== 步骤 ===")
for j in get(f"{BASE}/actions/runs/{run['id']}/jobs")["jobs"]:
    for s in j["steps"]:
        c = str(s["conclusion"])
        if c not in ("success", "skipped"):
            print(f"  {c:>9}  {s['name']}")

print("\n=== 错误注解 ===")
for j in get(f"{BASE}/actions/runs/{run['id']}/jobs")["jobs"]:
    try:
        anns = get(f"{BASE}/check-runs/{j['id']}/annotations")
    except Exception as exc:  # noqa: BLE001
        print("  读取失败:", str(exc)[:80])
        continue
    for a in anns:
        if a.get("annotation_level") != "failure":
            continue
        msg = a.get("message", "")
        if "详见上方" in msg or "见上方注解" in msg:
            continue
        print(" *", msg[:400])
