# 血压记录 · 安卓应用（Python + Bootstrap + Chart.js）

在安卓手机上运行的血压记录应用：Python（Flask）做后端，Bootstrap 5 做移动端 UI，Chart.js 画血压趋势曲线，SQLite 存储数据。
两种在安卓上运行的方式：**Termux 直接运行**（几分钟可用）或 **Buildozer 打包成 APK**（真·独立 App）。

## 安装到手机（三种方式，任选其一）

### 方式 A：Termux 直接跑（不用打包，最快）

1. 手机装 [Termux](https://f-droid.org/packages/com.termux/)（装 F-Droid 版，Play 商店版本已停止更新）。
2. 把整个项目文件夹拷到手机（USB 传、微信/QQ 传、或用 `termux-setup-storage` 后放 `/sdcard/bp`）。
3. 打开 Termux：

```bash
termux-setup-storage          # 申请存储权限，弹窗点允许
cd /sdcard/bp                 # 换成你的目录
bash run_android.sh           # 自动装 python + flask 并启动
```

4. 手机浏览器打开 `http://127.0.0.1:5000` 即可使用。
5. 想做成桌面图标：再装 **Termux:Widget**，在 `~/.shortcuts/` 建文件 `血压记录`：

```bash
mkdir -p ~/.shortcuts
echo 'cd /sdcard/bp && bash run_android.sh' > ~/.shortcuts/血压记录
chmod +x ~/.shortcuts/血压记录
```

之后在桌面添加 Termux:Widget 小组件，点一下就能启动（后台常驻可在 Termux 通知里锁定）。

> 注意：Termux 默认不对外网开放端口，所以只能用手机本机 `127.0.0.1` 访问，这正好保证数据不外泄。
> 关闭应用：在 Termux 里 Ctrl+C，或 `pkill -f main.py`。

### 方式 B：打包成 APK 安装（真正的独立 App）

本机是 Windows 且没有 WSL，无法直接构建（Buildozer 需要 Linux）。两条路：

**B1 · 用 GitHub Actions 云构建（推荐，不用装 Linux）**

1. 把项目推到 GitHub：
```bash
git init && git add . && git commit -m "血压记录 App"
git remote add origin https://github.com/<你的账号>/<仓库名>.git
git push -u origin main
```
2. 打开仓库 → **Actions** → 左侧 `Build Android APK` → **Run workflow**。
3. 约 15–30 分钟构建完成后，在 Artifacts 下载 `blood-pressure-apk`（内含 `.apk`）。
4. 把 APK 传到手机（微信/QQ 文件传输、USB 拷贝、或上传网盘下载），点开安装。
   - 提示「未知来源应用」→ 允许本次安装（设置 → 安装未知应用 → 对应来源允许）。
5. 桌面出现「血压记录」图标，点开即用，无需联网、无需电脑。

**B2 · 本机装 WSL 构建**

```powershell
wsl --install -d Ubuntu-22.04     # 需重启；完成后打开 Ubuntu
```
在 Ubuntu 里：
```bash
sudo apt update && sudo apt install -y python3-pip openjdk-17-jdk autoconf libtool pkg-config zlib1g-dev zip unzip git
pip3 install --user buildozer cython
cd /mnt/d/cv_coding/cv_song
buildozer -v android debug
```
产物：`bin/血压记录-1.0.0-debug.apk`，拷到手机安装。
（可选，用 adb 直接装：电脑装 platform-tools → `adb install bin/*.apk`）

### 方式 C：局域网 PWA（不安装，也能像 App 一样用）

电脑上 `python app.py`（或 `--port 5055`），手机与电脑同一 WiFi，手机浏览器打开终端打印的局域网地址，菜单选「添加到主屏幕」，即获得全屏图标。缺点是电脑必须开着。

---

## 功能

- 每日记录：日期、时间、收缩压、舒张压、心率、备注
- 曲线图趋势：收缩压 / 舒张压 / 心率三条曲线可单独开关，带 140 / 90 参考线
- 时间范围：7 天 / 30 天 / 90 天 / 全部
- 统计卡片：平均值、最高、最低、记录条数
- 自动分级（参考《中国高血压防治指南》）：正常 / 正常高值 / 1-3 级高血压，并给出健康提示
- 记录管理：日期区间筛选、分页、编辑、删除、导出 CSV
- 全部静态资源（Bootstrap、Chart.js）已本地化，手机上无需联网

## 方式一：安卓手机 Termux 直接运行（推荐，最快）

1. 手机安装 [Termux](https://f-droid.org/packages/com.termux/)（F-Droid 版本）。
2. 把整个项目目录复制到手机（例如 `/sdcard/bp` 或 Termux 主目录）。
3. 在 Termux 中执行：

```bash
termux-setup-storage
cd /sdcard/bp          # 换成你的目录
bash run_android.sh    # 自动安装 python + flask 并启动
```

4. 启动后手机浏览器打开 `http://127.0.0.1:5000` 即可使用；可安装 **Termux:Widget** 建桌面快捷方式一键启动。

> 说明：Termux 默认不允许外部访问端口，用本机 127.0.0.1 最安全；数据保存在手机本地 SQLite 文件中。

## 方式二：打包成安卓 APK

需在 Linux / WSL（本机未安装 WSL，可用 WSL 或云服务器/GitHub Actions）执行：

```bash
pip install buildozer
sudo apt install -y openjdk-17-jdk autoconf libtool pkg-config zip unzip
buildozer -v android debug      # 产物 bin/血压记录-1.0.0-debug.apk
```

关键配置在 `buildozer.spec`：

```
android.bootstrap = webview       # APK 内运行 Flask，界面用 Android WebView 显示
requirements = python3,flask
android.permissions = INTERNET
```

WebView 默认加载 `http://127.0.0.1:5000/`，与 `main.py` 中 `BP_PORT`（默认 5000）一致；
若用 `p4a --port 8080` 改端口，请同时设置环境变量 `BP_PORT=8080`。

## 桌面预览 / 局域网调试

```bash
pip install -r requirements.txt
python app.py            # 默认 5000 端口
python app.py --port 5055
python app.py --seed     # 写入 30 天演示数据（可先用它看曲线效果）
```

启动后打印本机与局域网地址，同一 WiFi 下手机浏览器可直接打开（PWA：菜单「添加到主屏幕」）。

## 目录结构

```
main.py            安卓 / APK 入口：后台启动本地 Web 服务
app.py             Flask 路由与业务逻辑
storage.py         SQLite 数据访问层
utils.py           血压分级与日期工具
templates/         Bootstrap 页面（首页趋势 / 记录表单 / 记录列表）
static/vendor/     Bootstrap 5、Chart.js（本地文件，离线可用）
static/css,js      自定义样式与曲线图交互
static/manifest.json, sw.js   PWA 离线支持
buildozer.spec     APK 打包配置
run_android.sh     Termux 一键运行脚本
```

## 数据

- 数据库：`bp.db`（Android 下写入应用私有目录，桌面下写入 `data/bp.db`）
- 备份：复制该文件或使用记录页「导出 CSV」
