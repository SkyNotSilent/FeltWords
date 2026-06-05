# 儿童拍照识词绘本 App 文档

本目录用于沉淀产品、设计和后续研发资料。

## 文件索引

- [PRD.md](./docs/PRD.md)：产品需求文档，包含定位、用户、功能范围、用户流程、数据结构、技术与安全要求。
- [DESIGN_SYSTEM.md](./docs/DESIGN_SYSTEM.md)：设计规范，包含视觉方向、颜色、字体、组件、动效、儿童安全设计原则。
- [FIGMA_DESIGN_BRIEF.md](./docs/FIGMA_DESIGN_BRIEF.md)：设计出图说明，可直接交给设计师或用于生成 Figma 初稿。

## 当前产品方向

一个面向儿童的英语启蒙 App：孩子拍摄身边物体，App 识别英文单词，并把这些单词变成带图片、朗读和自动播放的小故事绘本。

整体视觉方向：软萌儿童 3D 绘本感 + 明亮学习 App 布局 + 轻量毛毡手作元素。

## Figma

- UI 风格稿：[Felt Words 儿童英语绘本 App - UI 风格稿](https://www.figma.com/design/KAXwdz2TuXBbNLYa87DeQJ)
- V2 精致风格稿在同一 Figma 文件内，画板名：`Felt Words UI Style Board V2 - refined`

## iOS 开发

当前第一版 SwiftUI 工程已初始化，包含：

- V2 风格首页、拍照页、识别结果页、绘本阅读器、我的绘本、单词本
- `AVCaptureSession` 真实相机预览与拍照
- 相册选图
- Agnes 多模态物体识别
- Agnes 图片转毛毡插图，失败时自动退回文生图
- Agnes 四页儿童英文故事生成
- iOS 系统英文语音朗读
- 本地单词本与绘本数据保存
- Agnes 20 次/分钟客户端限流

本地运行：

1. 安装 Xcode 与对应 iOS Simulator runtime。
2. 安装 XcodeGen：`brew install xcodegen`。
3. 将 `Config/Secrets.xcconfig.example` 复制为 `Config/Secrets.xcconfig`，填写本地 Agnes API Key。
4. 执行 `xcodegen generate`。
5. 打开 `FeltWords.xcodeproj`，选择模拟器或真机运行。真实相机拍照需使用真机验证。

若命令行仍指向 Command Line Tools，可在终端执行：

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

### 命令行构建与模拟器验收

无需打开 Xcode 也可构建、安装、运行并截图验收（`UDID` 用 `xcrun simctl list devices` 查询）：

```bash
UDID=<iPhone 模拟器 UDID>
APP=~/Library/Developer/Xcode/DerivedData/FeltWords-*/Build/Products/Debug-iphonesimulator/毛毛英语绘本.app

xcrun simctl boot "$UDID"; open -a Simulator
xcodebuild -project FeltWords.xcodeproj -scheme FeltWords \
  -destination "platform=iOS Simulator,id=$UDID" -configuration Debug build
xcrun simctl install "$UDID" "$APP"
xcrun simctl launch  "$UDID" com.mima.feltwords
xcrun simctl io "$UDID" screenshot /tmp/home.png   # 直接抓取设备画面，不依赖窗口/录屏权限

# 模拟器无相机，拍照页会自动引导“从相册选择”。先放一张测试图进相册：
xcrun simctl addmedia "$UDID" /path/to/photo.jpg
```

构建产物里 `Info.plist` 的 `AGNES_API_KEY` 应为真实 Key（而非空或 `$(AGNES_API_KEY)`）：
`/usr/libexec/PlistBuddy -c 'Print :AGNES_API_KEY' "$APP/Info.plist"`。

### 常见问题排错

- **模拟器 `boot` 时 CoreSimulatorService 崩溃**（`Failed to create remote proxy for bundle instance`，`simctl list` 正常但 `boot` 必崩）：
  CoreSimulator 框架的 DeviceIO 二进制签名损坏。先确认 `codesign --verify /Library/Developer/PrivateFrameworks/CoreSimulator.framework` 报错，再用 Xcode 自带干净副本覆盖（`installer -pkg` 会按 BOM 跳过，必须直接覆盖）：
  ```bash
  sudo bash -c '
    W=$(mktemp -d); pkgutil --expand-full /Applications/Xcode.app/Contents/Resources/Packages/XcodeSystemResources.pkg "$W/x"
    SRC="$W/x/Payload/Library/Developer/PrivateFrameworks/CoreSimulator.framework"
    FW=/Library/Developer/PrivateFrameworks/CoreSimulator.framework
    xcrun simctl shutdown all 2>/dev/null; killall -9 com.apple.CoreSimulator.CoreSimulatorService 2>/dev/null
    mv "$FW" "$FW.broken-bak" && ditto "$SRC" "$FW" && codesign --verify "$FW" && echo OK
  '
  ```
- **运行时报“请先在 Config/Secrets.xcconfig 中配置 Agnes API Key”**：
  `Config/Base.xcconfig`（工程的 base configuration）需 `#include? "Secrets.xcconfig"`，且二者同在 `Config/` 目录；若 `Base.xcconfig` 被移入子目录，相对 include 会失效导致 Key 注入为空。
- **拍照页在模拟器一片黑**：属预期——模拟器无相机，已自动回退到“从相册选择”。真实相机需真机验证。

Agnes 接口细节见 [AGNES_INTEGRATION.md](./docs/AGNES_INTEGRATION.md)。完整排错与运行验收记录见 [DEV_LOG.md](./docs/DEV_LOG.md)。
