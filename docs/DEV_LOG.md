# 开发日志

## 2026-06-05 - 关键交互与 Liquid Glass 优化

- 修正主题切换三态循环反馈不明确的问题：点击始终在浅色/深色间切换，长按可选择跟随时间，并持久化用户选择。
- 单词本删除模式保持原行布局，仅平滑替换右侧操作；删除后提供 5 秒毛玻璃撤销提示，支持一轮连续删除整体恢复。
- 首页四卡片保留原布局，增加滑动聚焦、按压反馈、触感反馈和玻璃图标。
- iOS 26 使用原生 Liquid Glass，iOS 17-25 使用材质与描边回退。

## 2026-06-05 - 首页全页面滑动卡片

- 将首页原来的“开始拍照 / 历史记录”双态入口扩展为所有主要页面的横向快捷卡片。
- 卡片顺序严格跟随底部主导航：首页内依次为开始拍照、我的绘本、单词本、历史记录。
- 每张卡显示页面图标、滑动提示与实时内容数量摘要，点击直接进入对应页面；不显示无实际意义的分页数字。

## 2026-06-05 - 自动识别历史与首页滑动入口

- 新增独立历史记录：识别成功后立即自动保存，图片生成完成后更新同一条记录。
- 历史记录按时间倒序展示图片、单词与存入时间，支持一键存入单词本和生成绘本。
- 底部导航在单词本右侧新增历史入口。
- 首页将“开始拍照”改为横向双态入口，右侧露出历史卡并提供左右滑动提示。
- 单词本继续作为用户主动收藏，不与自动历史混用。
- 单词本优先展示保存时的毛毡图，并为缺图旧单词提供现有绘本关联与补图能力。

## 2026-06-05 - SwiftUI 第一版功能工程

- 使用 XcodeGen 初始化 `FeltWords.xcodeproj`，最低系统 iOS 17。
- 按 V2 设计基线实现首页、标准四角取景框拍照页、单词结果、绘本阅读、我的绘本和单词本。
- 接入 `AVCaptureSession` 真实相机、相册选图和拍照结果处理。
- 接入 Agnes `agnes-2.0-flash` 多模态识别与故事生成。
- 接入 Agnes `agnes-image-2.1-flash` 图片转毛毡插图，并增加文生图回退。
- 使用 `AVSpeechSynthesizer` 实现免费英文单词与绘本句子朗读。
- 增加本地数据保存、20 次/分钟请求限流与 API Key 本地配置隔离。
- 增加设备端 Vision 人脸检测，人物照片不会上传到 Agnes。
- 生成的毛毡插图会下载到应用本地目录，单词本和绘本不依赖远程临时 URL。
- Agnes 实测：文本、图片生成、多模态识别和 data URL 图片转图片均成功。
- 最终联调：data URL 多模态识别返回 `apple`，图片转图片返回毛毡插图 URL，模型列表仍包含所用文本与图片模型。
- 构建环境：完整 Xcode 已安装，但最初缺少 iOS Simulator runtime；已启动 iOS 26.5 arm64 runtime 下载。
- 已安装 iOS 26.5 Simulator runtime 并完成模拟器目标编译；显式 Info.plist 确保相机权限文案与本地 Agnes Key 正确注入。
- 当前机器的系统级 `xcode-select` 仍指向 Command Line Tools；模拟器启动会中断，需要用户使用管理员权限切换到完整 Xcode 或重启后再做视觉验收。命令级 `DEVELOPER_DIR` 下编译已成功。

本文件用于保存产品、设计和开发阶段的重要版本记录。普通提交会由 Git hook 自动追加 staged 文件摘要；重要方向变更需要手动补充背景和决策。

## 2026-06-05 - 模拟器运行验收与稳定性修复

### 模拟器无法启动的真正根因（排查记录）
- 现象：`xcrun simctl boot` 时 CoreSimulatorService 崩溃（EXC_CRASH/SIGABRT），`simctl list` 正常但 `boot` 必崩。
- 排错排除的两个错误假设：①`xcode-select` 指向 CLT / 切 Xcode 后服务残留 → 重启 Mac 无效；②第三方 HAL 声卡（ToDesk/Parrot）冲突 → 移走驱动无效。
- 真正根因：CoreSimulator.framework 的全部 DeviceIO 二进制（SimAudioProcessorService / SimRenderServer / SimMetalHost / SimStreamProcessorService / GPUToolsSimPortVendor 等）代码签名校验失败——内嵌 Apple 正式签名但字节不匹配，属安装后被改动（疑似 Xcode 更新中断/部分更新）。boot 时音频 bundle 的 XPC 启动路径严格校验签名失败 → 报 `Failed to create remote proxy for bundle instance.`（SimAudioProcessorServices.m:166）→ service abort。
- 修复：用 Xcode 自带 `XcodeSystemResources.pkg` 内的干净副本 `ditto` 覆盖 `/Library/Developer/PrivateFrameworks/CoreSimulator.framework`（`installer -pkg` 会按 BOM 跳过，需直接覆盖）。覆盖后 `codesign --verify` 全部通过，模拟器正常 boot。旧框架备份在 `CoreSimulator.framework.broken-bak`。

### 稳定性 / 异常处理修复
- `CameraService`：模拟器/无后置相机/配置失败时不再黑屏静默，自动置 `cameraUnavailable` 并在 UI 引导“从相册选择”；快门在会话未就绪时也触发该引导。
- `CameraScreen`：相册图片加载失败不再 `try?` 静默吞错，给中文提示；识别失败时仅对自有 `AgnesError` 显示其中文文案，其余系统底层错误（如 Vision 推理失败）统一兜底为“毛毛刚才没看清楚，请再试一次吧。”，不把英文错误抛给孩子。
- `WordResultView`：毛毡插图（增强项）生成失败时静默回退到原图，不再弹打扰性 alert。
- `PhotoSafetyService`：iOS 模拟器无法创建 Vision 人脸检测推理上下文（报 `Could not create inference context`），改为模拟器跳过检测以便联调；真机保留严格人脸拦截。
- `Config/.../Base.xcconfig`：该文件被移入 `Config/New Group/` 子目录后，相对 `#include? "Secrets.xcconfig"` 失效导致 `AGNES_API_KEY` 注入为空、运行时报缺 Key。补加 `#include? "../Secrets.xcconfig"` 回退，兼容两种位置。（遗留小问题：`New Group` 目录命名需后续清理）

### 模拟器逐页运行验收（iPhone 17 / iOS 26.5，真实 Agnes 接口）
- 首页：V2 黄色主题、毛毡熊、今日任务卡、底部四 Tab 渲染正常。
- 拍一拍：模拟器无相机 → 正确显示“从相册选择”引导（稳定性修复生效）。
- 完整链路：相册苹果照片 → 识别 `apple/苹果` + 例句 + 关联词（fruit/tape/pen）→ img2img 毛毡插图 → 四页故事（每页文本不同、共用插图）→ 翻页/朗读 UI。
- 我的绘本：生成的「The Red Apple」已持久化，重启后仍在。
- 单词本：「加入单词本」后 `apple/苹果/例句` 入库，重启后仍在。
- 验收方法已沉淀到 README「命令行构建与模拟器验收 / 常见问题排错」，含 CoreSimulator 修复脚本与 Key 注入排查。
- 模拟器 UI 自动化用 `cliclick` + 动态读取窗口坐标（`CGWindowListCopyWindowInfo`）；注意 Simulator 窗口可能被系统移动，每次点击前需重读窗口原点，否则坐标偏移点不中。

### 工程整洁性清理
- 将 `Base.xcconfig` 从误建的 `Config/New Group/` 移回 `Config/` 根目录，删除空的 New Group 组（同步更新 pbxproj 的 Config group 引用），include 还原为简洁的 `#include? "Secrets.xcconfig"`。重新构建通过、Key 注入正常。

## 2026-06-05

### 初始化产品与设计资料

- 创建 PRD、设计规范和 Figma 出稿 brief。
- 明确产品方向：拍照识物、英文单词、单词本、毛毡小绘本、自动朗读。
- 修正视觉方向为“毛毡元素”，避免误解为神秘或宗教化元素。
- 创建 Figma UI 风格稿：https://www.figma.com/design/KAXwdz2TuXBbNLYa87DeQJ

### 版本管理约定

- 初始化 Git 仓库。
- 增加 `AGENTS.md`，写入 Agent 工作约定。
- 增加 `.githooks/pre-commit`，用于提交前自动记录 staged 文件摘要。

### 自动提交记录 - 2026-06-05 10:43:53 +0800

```text
A	.githooks/pre-commit
A	AGENTS.md
A	README.md
A	docs/DESIGN_SYSTEM.md
A	docs/DEV_LOG.md
A	docs/FIGMA_DESIGN_BRIEF.md
A	docs/PRD.md
```

### Figma V2 精致风格稿

- 在同一 Figma 文件中新增 `Felt Words UI Style Board V2 - refined`。
- 将首页底部入口从抽象图标升级为更形象的毛毡相机、打开的绘本、单词卡。
- 优化拍照页：增加识别标签、相册图标、完整快门层次和更明确的取景反馈。
- 新增“我的绘本”列表页，用于表达绘本资产沉淀和再次阅读入口。
- 保留 V1 作为对比，不覆盖旧版本。

### 自动提交记录 - 2026-06-05 10:54:20 +0800

```text
M	README.md
M	docs/DEV_LOG.md
```

### Figma 拍照页错乱修正

- 根据视觉反馈，重建 V2 的拍照页 frame：`V2 02 Camera refined / fixed layout`。
- 修正取景框线条切入苹果主体的问题，让四角框避开识别对象。
- 重新整理底部控制区：关闭按钮、快门、相册入口分区更清楚。
- 将右下相册入口改为更形象的图片堆叠图标。
- 增加顶部轻状态和底部暗色控制层，让页面更接近真实相机 App。
- Figma 修改已写入文件，但截图复核时触发 Figma MCP Starter plan 调用上限，需稍后或升级额度后继续抓图验收。

### 自动提交记录 - 2026-06-05 10:57:55 +0800

```text
M	docs/DEV_LOG.md
```

### 拍照页取景框规范修正

- 明确拍照页不需要复杂识别框效果。
- 取景框规范改为屏幕中间居中的标准正方形，只显示四个角。
- 四角线不能切入被识别物体主体。
- 拍照页顶部不应出现刘海屏、状态条、横向胶囊条或“识别物体”标题。
- 已更新 `docs/DESIGN_SYSTEM.md` 和 `docs/FIGMA_DESIGN_BRIEF.md`。
- 尝试同步修改 Figma 时仍触发 Figma MCP Starter plan 调用上限，需要稍后恢复额度后再改画板。

### 自动提交记录 - 2026-06-05 11:02:14 +0800

```text
M	docs/DESIGN_SYSTEM.md
M	docs/DEV_LOG.md
M	docs/FIGMA_DESIGN_BRIEF.md
```

### 自动提交记录 - 2026-06-05 11:36:19 +0800

```text
A	.gitignore
A	Config/Base.xcconfig
A	Config/Secrets.xcconfig.example
A	FeltWords.xcodeproj/project.pbxproj
A	FeltWords.xcodeproj/project.xcworkspace/contents.xcworkspacedata
A	FeltWords/App/AppModel.swift
A	FeltWords/App/FeltWordsApp.swift
A	FeltWords/Components/CameraPreview.swift
A	FeltWords/Components/Theme.swift
A	FeltWords/Models/Models.swift
A	FeltWords/Services/AgnesAPIService.swift
A	FeltWords/Services/CameraService.swift
A	FeltWords/Services/LocalStore.swift
A	FeltWords/Services/RequestRateLimiter.swift
A	FeltWords/Services/SpeechService.swift
A	FeltWords/Views/CameraScreen.swift
A	FeltWords/Views/HomeView.swift
A	FeltWords/Views/RootTabView.swift
A	FeltWords/Views/StoryViews.swift
A	FeltWords/Views/WordResultView.swift
A	FeltWords/Views/WordbookView.swift
M	README.md
A	docs/AGNES_INTEGRATION.md
M	docs/DEV_LOG.md
A	project.yml
```

### 自动提交记录 - 2026-06-05 11:39:21 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
M	FeltWords/Services/AgnesAPIService.swift
M	FeltWords/Services/CameraService.swift
A	FeltWords/Services/PhotoSafetyService.swift
M	FeltWords/Views/CameraScreen.swift
M	docs/AGNES_INTEGRATION.md
M	docs/DEV_LOG.md
```

### 自动提交记录 - 2026-06-05 11:42:13 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
A	FeltWords/Components/StoredImage.swift
M	FeltWords/Services/AgnesAPIService.swift
A	FeltWords/Services/GeneratedImageStore.swift
M	FeltWords/Views/StoryViews.swift
M	FeltWords/Views/WordResultView.swift
M	docs/DEV_LOG.md
```

### 自动提交记录 - 2026-06-05 12:21:40 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
A	FeltWords/Info.plist
M	FeltWords/Services/CameraService.swift
M	FeltWords/Services/PhotoSafetyService.swift
M	README.md
M	docs/DEV_LOG.md
M	project.yml
```

### 自动提交记录 - 2026-06-05 12:22:16 +0800

```text
M	docs/DEV_LOG.md
```

### 自动提交记录 - 2026-06-05 14:09:31 +0800

```text
D	Config/Base.xcconfig
A	Config/New Group/Base.xcconfig
M	FeltWords.xcodeproj/project.pbxproj
M	FeltWords/Services/CameraService.swift
M	FeltWords/Services/PhotoSafetyService.swift
M	FeltWords/Views/CameraScreen.swift
M	FeltWords/Views/WordResultView.swift
M	docs/DEV_LOG.md
```

### 自动提交记录 - 2026-06-05 14:16:15 +0800

```text
A	Config/Base.xcconfig
D	Config/New Group/Base.xcconfig
M	FeltWords.xcodeproj/project.pbxproj
M	README.md
M	docs/DEV_LOG.md
```

### 自动提交记录 - 2026-06-05 14:25:18 +0800

```text
M	FeltWords/Views/CameraScreen.swift
```

### 自动提交记录 - 2026-06-05 14:40:36 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
A	FeltWords/Components/IllustrationLoadingView.swift
M	FeltWords/Components/StoredImage.swift
M	FeltWords/Services/GeneratedImageStore.swift
M	FeltWords/Views/WordResultView.swift
```

### 自动提交记录 - 2026-06-05 14:58:27 +0800

```text
M	FeltWords/Components/IllustrationLoadingView.swift
M	FeltWords/Views/CameraScreen.swift
M	FeltWords/Views/WordResultView.swift
```

### 自动提交记录 - 2026-06-05 16:22:03 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
M	FeltWords/App/AppModel.swift
M	FeltWords/App/FeltWordsApp.swift
M	FeltWords/Components/Theme.swift
M	FeltWords/Models/Models.swift
M	FeltWords/Services/AgnesAPIService.swift
M	FeltWords/Services/LocalStore.swift
A	FeltWords/Services/ProfileStore.swift
M	FeltWords/Services/SpeechService.swift
A	FeltWords/Services/WeatherService.swift
M	FeltWords/Views/HomeView.swift
M	FeltWords/Views/StoryViews.swift
M	FeltWords/Views/WordResultView.swift
M	FeltWords/Views/WordbookView.swift
```

### 自动提交记录 - 2026-06-05 16:50:46 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
M	FeltWords/App/AppModel.swift
M	FeltWords/Models/Models.swift
M	FeltWords/Services/GeneratedImageStore.swift
M	FeltWords/Services/LocalStore.swift
A	FeltWords/Views/HistoryView.swift
M	FeltWords/Views/HomeView.swift
M	FeltWords/Views/RootTabView.swift
M	FeltWords/Views/WordResultView.swift
M	FeltWords/Views/WordbookView.swift
M	docs/DESIGN_SYSTEM.md
M	docs/DEV_LOG.md
M	docs/FIGMA_DESIGN_BRIEF.md
M	docs/PRD.md
```

### 自动提交记录 - 2026-06-05 17:04:10 +0800

```text
M	FeltWords/Views/HomeView.swift
M	docs/DESIGN_SYSTEM.md
M	docs/DEV_LOG.md
M	docs/FIGMA_DESIGN_BRIEF.md
M	docs/PRD.md
```

### 自动提交记录 - 2026-06-05 17:08:48 +0800

```text
M	FeltWords/Views/HomeView.swift
M	docs/DESIGN_SYSTEM.md
M	docs/DEV_LOG.md
M	docs/FIGMA_DESIGN_BRIEF.md
```

### 自动提交记录 - 2026-06-05 17:19:17 +0800

```text
M	FeltWords/Services/WeatherService.swift
M	FeltWords/Views/HomeView.swift
```

### 自动提交记录 - 2026-06-05 17:36:34 +0800

```text
M	FeltWords/Views/WordbookView.swift
```

### 自动提交记录 - 2026-06-05 17:53:10 +0800

```text
M	FeltWords/App/AppModel.swift
M	FeltWords/Components/Theme.swift
M	FeltWords/Services/WeatherService.swift
M	FeltWords/Views/HomeView.swift
M	FeltWords/Views/WordbookView.swift
M	docs/DESIGN_SYSTEM.md
M	docs/DEV_LOG.md
```

## 2026-06-05 绘本播放结束与重播交互

- 绘本自动播放到最后一页并完成朗读后，自动退出播放状态，控制按钮恢复为播放图标。
- 停留在最后一页时再次点击播放，会先回到第一页，再从头连续播放。
- 全局英文朗读语速从 `0.42` 调整为 `0.36`，方便低龄儿童听清并跟读。

### 自动提交记录 - 2026-06-05 18:02:00 +0800

```text
M	FeltWords/Services/SpeechService.swift
M	FeltWords/Views/StoryViews.swift
M	docs/DEV_LOG.md
```

## 2026-06-05 首页头像删除交互

- 未上传头像时，右下角显示加号，点击头像打开图片选择器。
- 上传头像后，右下角加号切换为删除按钮；点击后删除本地头像并恢复默认小熊与加号。
- 点击头像主体仍可直接更换图片，删除按钮独立响应。

### 自动提交记录 - 2026-06-05 18:03:57 +0800

```text
M	FeltWords/App/AppModel.swift
M	FeltWords/Services/ProfileStore.swift
M	FeltWords/Views/HomeView.swift
M	docs/DEV_LOG.md
```

## 2026-06-05 绘本删除模式

- 绘本页删除方式与单词本统一：顶部垃圾桶进入管理状态，完成按钮退出。
- 删除状态下已有绘本卡片持续轻微抖动，并显示独立红色删除按钮；生成中的绘本不参与删除。
- 删除后提供 5 秒撤销，可连续删除并按原顺序恢复。

## 2026-06-05 Agnes IP 视觉升级

- 使用 Agnes `agnes-image-2.1-flash` 生成统一角色母图，并通过 img2img 派生首页四张快捷卡与空状态场景。
- 品牌 IP 固定为毛毡女孩、男孩与戴圆眼镜的小熊毛毛，统一黄色披肩、暖橙、天蓝和薄荷配色。
- 首页增加可轻点或下拉的绳子入口与每日状态舞台，展示当天发现、收藏单词与生成绘本数量；主要页面空状态统一使用 IP 插画。
- 保留 SF Symbols 作为功能图标，生成式插画只承担品牌场景和氛围表达。

## 2026-06-07 首页每日主题 IP 场景

- 使用 Agnes `agnes-image-2.1-flash` 和现有 IP 母图生成五张每日主题场景：好好吃饭、快乐学习、一起玩耍、整理物品与睡前准备。
- 场景保持女孩、男孩与圆眼镜小熊毛毛的角色造型、黄色披肩和暖色毛毡风格，图片不包含文字，后续由 SwiftUI 叠加正向引导文案。
- 下拉舞台采用按住查看、松手回弹的交互，因此移除舞台右上角关闭按钮。
- 每次 App 完整重新启动时按固定顺序轮换默认母图和五张主题图；同一次运行期间保持稳定，避免页面重绘导致内容跳变。
- 首页拉绳整体左移，避免与右上角天气主题按钮重叠。

### 自动提交记录 - 2026-06-05 18:11:31 +0800

```text
M	FeltWords/App/AppModel.swift
M	FeltWords/Views/StoryViews.swift
M	docs/DEV_LOG.md
```

### 自动提交记录 - 2026-06-06 00:02:20 +0800

```text
M	FeltWords.xcodeproj/project.pbxproj
M	FeltWords/App/AppModel.swift
A	FeltWords/Components/MascotViews.swift
A	FeltWords/Resources/Mascot/card-camera.png
A	FeltWords/Resources/Mascot/card-history.png
A	FeltWords/Resources/Mascot/card-stories.png
A	FeltWords/Resources/Mascot/card-words.png
A	FeltWords/Resources/Mascot/empty-state.png
A	FeltWords/Resources/Mascot/mascot-key-art.png
M	FeltWords/Views/HistoryView.swift
M	FeltWords/Views/HomeView.swift
M	FeltWords/Views/StoryViews.swift
M	FeltWords/Views/WordbookView.swift
M	docs/AGNES_INTEGRATION.md
M	docs/DESIGN_SYSTEM.md
M	docs/DEV_LOG.md
M	docs/FIGMA_DESIGN_BRIEF.md
M	project.yml
```

### 自动提交记录 - 2026-06-07 22:32:35 +0800

```text
M	.gitignore
M	FeltWords.xcodeproj/project.pbxproj
M	FeltWords/Components/MascotViews.swift
A	FeltWords/Resources/Mascot/DailyScenes/daily-bedtime.png
A	FeltWords/Resources/Mascot/DailyScenes/daily-eating.png
A	FeltWords/Resources/Mascot/DailyScenes/daily-learning.png
A	FeltWords/Resources/Mascot/DailyScenes/daily-playing.png
A	FeltWords/Resources/Mascot/DailyScenes/daily-tidying.png
M	FeltWords/Views/HomeView.swift
M	docs/AGNES_INTEGRATION.md
M	docs/DEV_LOG.md
A	scripts/generate_mascot_daily_scenes.rb
```

### 自动提交记录 - 2026-06-07 22:44:59 +0800

```text
M	FeltWords/App/AppModel.swift
M	FeltWords/Components/MascotViews.swift
M	FeltWords/Views/HomeView.swift
M	docs/DEV_LOG.md
```
## 2026-06-08 Android UI 隔离与首轮重构

- 将另一个 AI 留下的未跟踪 `android/` 工程和迁移文档完整迁移到独立 worktree `/Users/mima1234/Documents/AI产品经理/agens_app-android`，分支为 `codex/android-ui-rebuild`；iOS `main` 工作区恢复干净。
- 以当前 iOS 模拟器首页截图为视觉基准，确认 Android 原实现存在 emoji 替代 IP、主题卡结构错误、任务文字重叠、入口卡无图、默认 Material 底栏等问题。
- 复用 iOS 已有的 11 张 Agnes 品牌/IP 图片，重做 Android 首页、下拉每日状态、四张入口卡、头像默认态、底栏与绘本/单词本/历史空状态。
- 下拉每日状态支持手势跟随与松手回弹；首页主题保持每次完整启动顺序轮换。
- 修复 Release 签名密码硬编码，改为从被忽略的 `local.properties` 注入。
- 验证：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:assembleDebug :app:assembleRelease` 构建成功；Debug APK 已安装到 `felt_pixel` 模拟器并完成首页、拉绳、空状态截图验收。

### 自动提交记录 - 2026-06-08 00:40:23 +0800

```text
M	.gitignore
A	android/.gitignore
A	android/app/build.gradle.kts
A	android/app/proguard-rules.pro
A	android/app/src/main/AndroidManifest.xml
A	android/app/src/main/java/com/mima/feltwords/FeltApplication.kt
A	android/app/src/main/java/com/mima/feltwords/MainActivity.kt
A	android/app/src/main/java/com/mima/feltwords/data/ServiceLocator.kt
A	android/app/src/main/java/com/mima/feltwords/data/api/AgnesApi.kt
A	android/app/src/main/java/com/mima/feltwords/data/api/AgnesDtos.kt
A	android/app/src/main/java/com/mima/feltwords/data/api/AgnesError.kt
A	android/app/src/main/java/com/mima/feltwords/data/api/AgnesRepository.kt
A	android/app/src/main/java/com/mima/feltwords/data/api/NetworkModule.kt
A	android/app/src/main/java/com/mima/feltwords/data/api/RateLimiter.kt
A	android/app/src/main/java/com/mima/feltwords/data/store/ImageStore.kt
A	android/app/src/main/java/com/mima/feltwords/data/store/LocalStore.kt
A	android/app/src/main/java/com/mima/feltwords/data/store/ProfileStore.kt
A	android/app/src/main/java/com/mima/feltwords/data/util/ImageUtils.kt
A	android/app/src/main/java/com/mima/feltwords/data/weather/WeatherRepository.kt
A	android/app/src/main/java/com/mima/feltwords/domain/model/Models.kt
A	android/app/src/main/java/com/mima/feltwords/speech/TtsManager.kt
A	android/app/src/main/java/com/mima/feltwords/ui/AppViewModel.kt
A	android/app/src/main/java/com/mima/feltwords/ui/capture/CameraScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/capture/CaptureViewModel.kt
A	android/app/src/main/java/com/mima/feltwords/ui/capture/WordResultScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/FeltButton.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/FeltCard.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/GlassCard.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/LoadingIllustration.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/MascotEmptyState.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/Skeleton.kt
A	android/app/src/main/java/com/mima/feltwords/ui/history/HistoryScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/home/HomeScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/root/RootScaffold.kt
A	android/app/src/main/java/com/mima/feltwords/ui/story/StoryLibraryScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/story/StoryReaderScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/theme/Color.kt
A	android/app/src/main/java/com/mima/feltwords/ui/theme/Theme.kt
A	android/app/src/main/java/com/mima/feltwords/ui/theme/Type.kt
A	android/app/src/main/java/com/mima/feltwords/ui/word/WordbookScreen.kt
A	android/app/src/main/res/drawable-nodpi/card_camera.png
A	android/app/src/main/res/drawable-nodpi/card_history.png
A	android/app/src/main/res/drawable-nodpi/card_stories.png
A	android/app/src/main/res/drawable-nodpi/card_words.png
A	android/app/src/main/res/drawable-nodpi/daily_bedtime.png
A	android/app/src/main/res/drawable-nodpi/daily_eating.png
A	android/app/src/main/res/drawable-nodpi/daily_learning.png
A	android/app/src/main/res/drawable-nodpi/daily_playing.png
A	android/app/src/main/res/drawable-nodpi/daily_tidying.png
A	android/app/src/main/res/drawable-nodpi/empty_state.png
A	android/app/src/main/res/drawable-nodpi/mascot_key_art.png
A	android/app/src/main/res/drawable/ic_launcher_foreground.xml
A	android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
A	android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
A	android/app/src/main/res/values/colors.xml
A	android/app/src/main/res/values/strings.xml
A	android/app/src/main/res/values/themes.xml
A	android/build.gradle.kts
A	android/gradle.properties
A	android/gradle/libs.versions.toml
A	android/gradle/wrapper/gradle-wrapper.jar
A	android/gradle/wrapper/gradle-wrapper.properties
A	android/gradlew
A	android/gradlew.bat
A	android/settings.gradle.kts
A	docs/ANDROID_MIGRATION.md
M	docs/DEV_LOG.md
```

## 2026-06-08 Android 核心链路、交互与视觉收口

- 继续仅在 `codex/android-ui-rebuild` 独立 worktree 修改 Android，未改动 iOS `main`。
- 统一拍照识别、历史记录、单词本和绘本的共享状态：识别后立即写入历史，毛毡图生成后回填历史；历史/结果页收藏后单词本立即刷新；绘本生成统一进入全局后台任务。
- 将识别结果改为“原照片 -> 毛毡绘本”双图加载布局；相机页使用四角对焦框、隐藏底栏并铺满系统状态栏区域。
- 修复阅读器自动播放：语速降为 `0.7`，末页完成后自动恢复播放态，末页再次点击从第一页重播；TTS 不可用或报错时不再永久卡在暂停态。
- 绘本删除模式增加抖动，绘本/单词连续删除使用单一可续期撤销窗口；移除生成卡片无意义的 `1/4` 页码文案。
- 主题切换改为持久化，天气昼夜加载后可驱动根主题刷新；天气图标按天气代码变化。每日 IP 图与 iOS 一致，按完整启动顺序轮换；下拉入口左移，避免遮挡天气。
- 首页四张入口卡、底栏、主题按钮和单词卡统一加入弹簧按压反馈；启动图标替换为 Agnes 品牌 IP。
- 增加集合状态纯函数与单元测试，覆盖单词去重前置、批量撤销恢复顺序、历史毛毡图回填。
- 模拟器实测覆盖：主题切换与重启持久化、IP 启动轮换、相机/识别结果、自动历史、即时收藏、后台绘本生成、绘本抖动删除与批量撤销、单词批量撤销、阅读器末页暂停与从头重播。
- 验证命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`。
- 下一步：真机检查相机方向与高分辨率图片内存占用，并根据 Android 真机字体渲染继续微调与 iOS 的尺寸差异。

### 自动提交记录 - 2026-06-08 01:39:45 +0800

```text
M	android/app/build.gradle.kts
M	android/app/src/main/java/com/mima/feltwords/data/ServiceLocator.kt
M	android/app/src/main/java/com/mima/feltwords/data/store/ProfileStore.kt
M	android/app/src/main/java/com/mima/feltwords/data/weather/WeatherRepository.kt
A	android/app/src/main/java/com/mima/feltwords/domain/model/CollectionOps.kt
M	android/app/src/main/java/com/mima/feltwords/speech/TtsManager.kt
M	android/app/src/main/java/com/mima/feltwords/ui/AppViewModel.kt
M	android/app/src/main/java/com/mima/feltwords/ui/capture/CameraScreen.kt
M	android/app/src/main/java/com/mima/feltwords/ui/capture/CaptureViewModel.kt
M	android/app/src/main/java/com/mima/feltwords/ui/capture/WordResultScreen.kt
A	android/app/src/main/java/com/mima/feltwords/ui/components/FeltPress.kt
M	android/app/src/main/java/com/mima/feltwords/ui/components/GlassCard.kt
M	android/app/src/main/java/com/mima/feltwords/ui/history/HistoryScreen.kt
M	android/app/src/main/java/com/mima/feltwords/ui/home/HomeScreen.kt
M	android/app/src/main/java/com/mima/feltwords/ui/root/RootScaffold.kt
M	android/app/src/main/java/com/mima/feltwords/ui/story/StoryLibraryScreen.kt
M	android/app/src/main/java/com/mima/feltwords/ui/story/StoryReaderScreen.kt
M	android/app/src/main/java/com/mima/feltwords/ui/word/WordbookScreen.kt
A	android/app/src/main/res/drawable-nodpi/launcher_mascot.png
M	android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
M	android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
A	android/app/src/test/java/com/mima/feltwords/domain/model/CollectionOpsTest.kt
M	android/gradle/libs.versions.toml
M	docs/DEV_LOG.md
```
