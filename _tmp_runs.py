"""临时脚本：尝试下载构建日志 artifact（用完即删）。"""
import json
import urllib.request

op = urllib.request.build_opener(urllib.request.ProxyHandler({"http": "127.0.0.1:7897"}))
API = "https://api.github.com/repos/cv-develop-calvin/bloodStress"


def get(url, raw=False):
    req = urllib.request.Request(url, headers={"User-Agent": "x", "Accept": "application/vnd.github+json"})
    data = op.open(req, timeout=60).read()
    return data if raw else json.loads(data)


run_id = 35544605470
arts = get(f"{API}/actions/runs/{run_id}/artifacts")
for a in arts["artifacts"]:
    print(a["id"], a["name"], a["size_in_bytes"])
    url = f"https://api.github.com/repos/cv-develop-calvin/bloodStress/actions/artifacts/{a['id']}/zip"
    try:
        data = get(url, raw=True)
        out = a["name"] + ".zip"
        open(out, "wb").write(data)
        print("   downloaded", out, len(data))
    except Exception as e:  # noqa: BLE001
        print("   ERR", str(e)[:120])
