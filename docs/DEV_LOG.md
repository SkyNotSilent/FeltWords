# 开发日志

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
