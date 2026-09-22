"""临时脚本：用代理补齐剩余 Gradle Wrapper 文件（用完即删）。"""
import os
import urllib.request

PROXY = "http://127.0.0.1:7897"
BASE = "https://raw.githubusercontent.com/gradle/gradle/v8.7.0"
TARGETS = [
    ("gradlew.bat", "gradlew.bat"),
    ("gradle/wrapper/gradle-wrapper.jar", os.path.join("gradle", "wrapper", "gradle-wrapper.jar")),
]

op = urllib.request.build_opener(urllib.request.ProxyHandler({"https": PROXY, "http": PROXY}))

for remote, local in TARGETS:
    if os.path.exists(local) and os.path.getsize(local) > 0:
        print(f"SKIP {local} 已存在")
        continue
    url = f"{BASE}/{remote}"
    try:
        data = op.open(urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"}), timeout=90).read()
        os.makedirs(os.path.dirname(local) or ".", exist_ok=True)
        with open(local, "wb") as f:
            f.write(data)
        print(f"OK   {local:40} {len(data)} bytes")
    except Exception as exc:  # noqa: BLE001
        print(f"FAIL {local:40} {str(exc)[:70]}")
