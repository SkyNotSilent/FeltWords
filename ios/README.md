# FeltWords iOS

当前 iOS 版本作为独立源码工程提供。

状态：**iOS Simulator Debug 编译通过，但当前版本尚未完成完整运行与真机验收。**

## 技术栈

- Swift + SwiftUI
- ObservableObject + async/await
- URLSession
- AVCaptureSession + PhotosUI
- AVSpeechSynthesizer
- UserDefaults + 本地图片存储

## 本地配置

```bash
cp Config/Secrets.xcconfig.example Config/Secrets.xcconfig
```

填写：

```text
AGNES_API_KEY = your-api-key
```

## 打开工程

```bash
open FeltWords.xcodeproj
```

## 命令行编译

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild -project FeltWords.xcodeproj \
  -scheme FeltWords \
  -sdk iphonesimulator \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO build
```

## 已知边界

- 当前提交未完成逐页运行验收。
- 真机相机、朗读、权限和性能必须单独验证。
- 正式发布前应将 AI API 调用迁移到自有后端。
