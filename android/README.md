# FeltWords Android

当前 Android 版本是仓库中的主要可运行与已验收版本。

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- ViewModel + StateFlow + Coroutines
- Retrofit + OkHttp
- CameraX + Photo Picker
- DataStore + 本地图片存储
- Edge TTS 音频生成与 MediaPlayer 播放

## 本地配置

复制示例文件：

```bash
cp local.properties.example local.properties
```

填写：

```properties
sdk.dir=/Users/your-name/Library/Android/sdk
AGNES_API_KEY=your-api-key
```

如需 Release 构建，还需要本地签名配置：

```properties
FELTWORDS_STORE_PASSWORD=your-password
FELTWORDS_KEY_PASSWORD=your-password
```

真实密钥、签名文件、APK 与 `local.properties` 均已被 Git 忽略。

## 构建

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 运行

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mima.feltwords/.MainActivity
```

## 已知边界

- 当前主要完成模拟器验收，仍需 Android 真机矩阵测试。
- 在线朗读依赖网络与第三方 TTS 服务可用性。
- 正式发布前应将 AI API 调用迁移到自有后端。
