"""临时脚本：查看图标提交后的构建结果（用完即删）。"""
import json
import urllib.request

op = urllib.request.build_opener(urllib.request.ProxyHandler({"http": "127.0.0.1:7897"}))
API = "https://api.github.com/repos/cv-develop-calvin/bloodStress"


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "x", "Accept": "application/vnd.github+json"})
    return json.load(op.open(req, timeout=30))


print("main:", get(API + "/commits/main")["sha"][:7])
for r in get(API + "/actions/runs?per_page=2")["workflow_runs"]:
    print(f"\nrun {r['id']} | {r['status']}/{r['conclusion']} | sha {r['head_sha'][:7]} | {r['created_at']}")
    for a in get(f"{API}/actions/runs/{r['id']}/artifacts")["artifacts"]:
        print(f"   artifact {a['name']} {a['size_in_bytes']}B")
    for j in get(f"{API}/actions/runs/{r['id']}/jobs")["jobs"]:
        for s in j["steps"]:
            if s.get("completed_at"):
                print(f"   {s['number']:>2} {s['name'][:32]:<32} {s['conclusion']}")
