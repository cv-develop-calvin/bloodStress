"""临时脚本：等待云端构建完成并输出步骤结果（用完即删）。"""
import json
import sys
import time
import urllib.request

op = urllib.request.build_opener(urllib.request.ProxyHandler({"http": "127.0.0.1:7897"}))
BASE = "https://api.github.com/repos/cv-develop-calvin/bloodStress"
TARGET = "fd3e17b"


def get(u):
    return json.load(op.open(urllib.request.Request(u, headers={"User-Agent": "x"}), timeout=30))


run = None
for _ in range(20):
    for r in get(BASE + "/actions/runs?per_page=6")["workflow_runs"]:
        if r["head_sha"].startswith(TARGET):
            run = r
            break
    if run:
        break
    time.sleep(5)

if not run:
    print("未找到该提交的运行记录")
    sys.exit(0)

print("run", run["id"], "created", run["created_at"])
for i in range(14):          # 最多约 14 分钟
    r = get(f"{BASE}/actions/runs/{run['id']}")
    print(f"[{i}] {r['status']} / {r['conclusion']}")
    if r["status"] == "completed":
        print("\n=== 步骤结果 ===")
        for j in get(f"{BASE}/actions/runs/{run['id']}/jobs")["jobs"]:
            for s in j["steps"]:
                print(f"  {str(s['conclusion']):>9}  {s['name']}")
        print("\n=== Releases ===")
        for x in get(BASE + "/releases")[:2]:
            print("tag=", x["tag_name"])
            for a in x.get("assets", []):
                print("   ", a["name"], round(a["size"] / 1048576, 1), "MB")
        break
    time.sleep(60)
else:
    print("仍在构建中")
