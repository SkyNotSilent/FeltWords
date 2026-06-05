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

Agnes 接口细节见 [AGNES_INTEGRATION.md](./docs/AGNES_INTEGRATION.md)。
