# 宋宝宝的记录

安卓原生健康记录应用：记录血压、用药、血常规与日常留言，数据全部保存在手机本地。

**技术栈**：Kotlin · Jetpack Compose + Material 3 · MVVM · Room(SQLite) · Hilt · Coroutines/Flow · Navigation Compose · MPAndroidChart · Google ML Kit · Coil

**主题**：星座战士风格（深空星夜背景 + 金色描边），全局使用 Noto Sans SC 字体。

## 功能

### 血压
- 记录收缩压 / 舒张压 / 心率 / 备注
- 自动分级判定（偏低 / 正常 / 正常高值 / 1–3 级高血压）并给出健康提示
- 趋势曲线：7 / 30 / 90 天 / 全部，三条曲线可单独开关，含 140 / 90 参考线
- 统计：平均值、最高、最低、记录数
- 列表支持日期区间筛选、编辑、删除

### 用药
- 药品档案：名称、剂量、单位、频次、多个服药时间点、起止日期、备注
- 服药打卡（可撤销），依从性统计（近 7 天）
- **到点提醒**：系统通知 + 手机默认铃声 + 震动，重启后自动恢复

### 血常规
- 13 项常用指标：白细胞、红细胞、血红蛋白、红细胞压积、MCV、MCH、MCHC、血小板、淋巴/中性/单核/嗜酸性粒细胞百分比、超敏 CRP
- 每项按参考区间自动判定偏低 / 正常 / 偏高
- 指标趋势曲线（含参考区间上下限）
- **拍照识别化验单**：调用相机或相册 → ML Kit 中文识别 → 解析指标 → 预填表单确认

### 笔记本
- 文字留言：标题、内容、心情、标签
- 照片：拍照 / 相册多选（最多 9 张），自动压缩并生成缩略图
- 搜索（标题 / 内容 / 标签）、标签筛选、照片墙

### 其他
- 关于页：版本信息、技术栈、更新日志

## 构建

用 Android Studio（Giraffe 或更高版本）打开项目根目录，等待 Gradle 同步后直接运行到手机或模拟器。

命令行构建（需 JDK 17）：

```bash
./gradlew assembleDebug      # 输出 app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # 发布版（需配置签名）
```

最低支持 Android 8.0（API 26），目标 Android 14（API 34），仅 arm64-v8a。

## 项目结构

```
app/src/main/java/org/bp/songbaobao/
├── data/
│   ├── local/          Room 数据库、Entity、DAO
│   └── repository/     数据仓库（血压 / 用药 / 血常规 / 笔记）
├── domain/             业务逻辑（血压分级、血常规指标、OCR 解析）
├── di/                 Hilt 模块
├── reminder/           用药提醒（AlarmManager、广播接收器、通知）
├── ui/
│   ├── theme/          主题配色、字体、Typography
│   ├── components/     通用组件与图表
│   ├── navigation/     路由定义与底部导航
│   └── screen/         各功能页面
└── util/               时间与格式化工具
```

## 数据

- 数据库：`songbaobao.db`，位于应用私有目录，卸载应用会一并删除
- 笔记照片：位于应用 `filesDir/photos`
- 数据结构与旧 Web 版一致，均为 SQLite，便于迁移

## 权限

| 权限 | 用途 |
|---|---|
| POST_NOTIFICATIONS | 用药提醒通知 |
| VIBRATE | 提醒震动 |
| RECEIVE_BOOT_COMPLETED | 重启后恢复提醒 |
| SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM | 精准到点提醒 |
| READ_MEDIA_IMAGES | 从相册选图（化验单、笔记照片） |

拍照使用系统相机（Photo Picker / TakePicture），不申请 CAMERA 权限。
