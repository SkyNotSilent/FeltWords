# 安卓版迁移方案（毛毛英语绘本 / FeltWords → Android）

> 目标：在安卓手机上可本地安装、可演示，质量接近上架水平。
> 现状：iOS 原生 SwiftUI 工程（约 3200 行）。本方案为**原生重写**，非自动转换。

## 2026-06-08 重构审查结论

- Android 代码已隔离到 `codex/android-ui-rebuild` 分支与独立 worktree，不进入 iOS `main` 工作区。
- 原迁移实现具备基本数据层、相机、识别、绘本、单词本和历史链路，但“逐像素对齐 iOS”的原结论不成立。
- 首页原先使用 emoji 代替品牌 IP，常驻主题卡破坏页面结构，今日任务存在文字重叠，四张入口卡无场景图，底栏直接使用 Material 默认样式。
- 本轮已复用 iOS 的 Agnes 品牌图，按 iOS 截图重做首页、下拉每日状态、入口卡、头像默认态、底栏和空状态。
- Release 签名密码已从 Gradle 源码移到被忽略的 `local.properties`。
- Debug 与 Release APK 均已构建成功，并在本地 `felt_pixel` Android 模拟器安装验收。

---

## 1. 技术选型

| 维度 | iOS 现状 | 安卓采用 | 说明 |
|------|----------|----------|------|
| 语言 | Swift 5 | **Kotlin** | 现代、与 Swift 思路接近 |
| UI 框架 | SwiftUI | **Jetpack Compose (Material 3)** | 同为声明式，迁移直观 |
| 最低系统 | iOS 17 | **Android 8.0 (API 26)+** | 覆盖绝大多数在用机型 |
| 编译目标 | — | **compileSdk 34** | 已安装 |
| 网络 | URLSession | **Retrofit + OkHttp** | 调 Agnes 同一套接口 |
| JSON | Codable | **kotlinx.serialization** | 与 Codable 等价 |
| 图片加载/缓存 | AsyncImage + 自建持久化 | **Coil** | 自带磁盘缓存 |
| 相机 | AVCaptureSession | **CameraX** | 官方推荐，稳定 |
| 相册选图 | PhotosUI | **Photo Picker (ActivityResult)** | 系统级，免权限 |
| 朗读 TTS | AVSpeechSynthesizer | **Android TextToSpeech** | 见 §5 音色处理 |
| 本地存储 | UserDefaults + 文件 | **DataStore + 内部存储目录** | 等价键值 + 文件 |
| 并发 | async/await + TaskGroup | **Coroutines + Flow** | 一一对应 |
| 状态管理 | ObservableObject | **ViewModel + StateFlow** | 等价 |

**为什么是原生而非 Flutter**：你最担心"效果打折 + 流畅度"。原生 Compose 在视觉还原和动效流畅度上确定性最高，且 Compose↔SwiftUI 几乎可直译。代价是 iOS 与安卓两套代码——但 iOS 你已完成，这代价已付。

---

## 2. 功能清单（逐项迁移对照）

| # | 功能 | iOS 实现位置 | 安卓方案 | 难度 | 效果是否打折 |
|---|------|-------------|----------|------|------|
| 1 | 拍照识别物体（多模态） | AgnesAPIService.recognize | Retrofit 调 chat/completions | 低 | 无（同接口） |
| 2 | 图片转毛毡插图（img2img + 文生图回退） | generateFeltImage | images/generations + extra_body | 低 | 无 |
| 3 | 四页英文故事生成 | generateStoryText | chat/completions | 低 | 无 |
| 4 | 连环画逐页插画（参考图保持一致，并行+进度回调） | generateIllustratedStory | Coroutines + Flow 进度 | 中 | 无 |
| 5 | 后台生成绘本（生成中占位卡片、失败重试） | AppModel.startStoryGeneration | ViewModel + StateFlow | 中 | 无 |
| 6 | 20 次/分钟客户端限流 | RequestRateLimiter | Mutex/Semaphore 协程实现 | 低 | 无 |
| 7 | 真机相机预览与拍照 | CameraScreen + CameraPreview | CameraX PreviewView | 中 | 无 |
| 8 | 相册选图 | PhotosUI | Photo Picker | 低 | 无 |
| 9 | 英文语音朗读（语速 0.36、音调 1.08） | SpeechService | TextToSpeech | 低 | ⚠️ 音色见 §5 |
| 10 | 自动播放翻页（读完才翻、暂停不误翻） | SpeechService onFinish | TTS UtteranceProgressListener | 中 | 无 |
| 11 | 单词本（去重、删除、撤销恢复） | WordbookView + AppModel | Compose + ViewModel | 中 | 无 |
| 12 | 我的绘本（抖动删除模式、撤销） | StoryViews | Compose 动画 | 中 | 无 |
| 13 | 历史记录 | HistoryView | Compose List | 低 | 无 |
| 14 | 首页（今日任务、头像增删恢复、吉祥物每日主题） | HomeView + MascotViews | Compose + Canvas | 中高 | 无 |
| 15 | 吉祥物 Agnes 形象（矢量绘制） | MascotViews | Compose Canvas/Vector | 中高 | 需重绘还原 |
| 16 | 毛毡视觉风格（动态浅/深色、毛玻璃、圆角、弹簧动效） | Theme.swift | Compose Theme + Modifier | 中 | 高度还原 |
| 17 | 生成中骨架/加载动画 | IllustrationLoadingView | Compose 动画 | 中 | 无 |
| 18 | 儿童照片安全（避开人物/敏感特征） | PhotoSafetyService + prompt | prompt 原样保留 | 低 | 无 |
| 19 | 本地数据持久化 | LocalStore/ProfileStore/GeneratedImageStore | DataStore + filesDir | 中 | 无 |
| 20 | 天气服务 | WeatherService | 按需移植（确认是否演示用到） | 低 | — |

**结论**：20 项里 19 项无效果损失；唯一需特殊处理的是 #9 朗读音色。

---

## 3. Agnes 接口契约（直接复用，零改动）

```
Base URL : https://apihub.agnes-ai.com/v1
鉴权     : Authorization: Bearer <AGNES_API_KEY>
超时     : 90s
限流     : 客户端 20 次 / 60 秒

POST /chat/completions
  - 文本：model=agnes-2.0-flash, messages[{role,content:String}]
  - 多模态：content:[{type:text,text}, {type:image_url,image_url:{url:"data:image/jpeg;base64,..."}}]
  - temperature / max_tokens

POST /images/generations
  - model=agnes-image-2.1-flash, prompt, size=1024x1024
  - img2img：tags=["img2img"], extra_body:{image:["data:..."], response_format:"url"}
  - 失败回退：去掉 tags/extra_body 做纯文生图
```

所有 prompt（识别 / 毛毡风格 / 故事生成 / 逐页插画）**原文照搬**，保证输出风格一致。

---

## 4. 视觉规范（忠实还原）

色板（浅色 / 深色 双主题，已从 Theme.swift 提取）：

| 名称 | 浅色 | 深色 | 用途 |
|------|------|------|------|
| yellow | #FFD21F | #241D10 | 主背景 |
| orange | #FF8A2A | #F5963A | 强调/按钮/选中 |
| cream | #FFF6D8 | #322B1E | 暖色卡片 |
| mint | #A9EBD6 | #2C5147 | 辅助 |
| sky | #BDEEFF | #223038 | 辅助 |
| pink | #FFB8C8 | #5E3540 | 辅助/吉祥物 |
| ink | #3B2D1F | #F6EAD0 | 主文字 |
| secondary | #8D7A56 | #B9A983 | 次文字 |
| surface | #FFFFFF | #342C1F | 白卡片 |

其它要素：
- 字体：圆润粗体（Compose 用 rounded 风格字体 + FontWeight.Bold）
- 按钮：圆角 22dp、按下缩放 0.97、56dp 高
- 卡片圆角 24dp；弹簧动效（response≈0.3–0.5, damping≈0.7）
- 毛玻璃：iOS 26 的 glassEffect → 安卓用半透明 + 模糊（RenderEffect/blur）等价
- 图标：iOS 用 SF Symbols → 安卓用 Material Icons 对应替换

---

## 5. 唯一风险点：朗读音色

- 安卓系统 TTS 英文默认音色不如 iOS 自然。
- 处理方案（按成本递增）：
  1. 用 Android TTS + 选用设备上较好的英文引擎/嗓音（免费、离线、最快）；
  2. 引导用户安装 Google 语音引擎获取更自然音色；
  3. 接云端 TTS（更自然，但需额外服务/成本）。
- 建议：先用方案 1 出 Demo，演示无碍；若你对音色不满意再升级。

---

## 6. 工程架构（安卓侧）

```
app/
├── data/
│   ├── api/        AgnesService (Retrofit) + DTO + RateLimiter
│   ├── store/      DataStore(words/stories/history/tasks) + 文件存储(图片/头像)
│   └── repo/       Repository 封装
├── domain/model/   RecognitionResult / LearnedWord / Storybook / StoryPage / DailyTask ...
├── speech/         TtsManager
├── camera/         CameraX 封装
├── ui/
│   ├── theme/      FeltTheme(色板/字体/形状/动效)
│   ├── components/ FeltButton / GlassCard / Mascot / LoadingIllustration
│   ├── home/  camera/  story/  word/  history/   (各屏 + ViewModel)
│   └── root/       底部 5 Tab 导航
└── MainActivity + Application
```

- AppModel（iOS）→ 拆成各屏 ViewModel + 共享 Repository。
- API Key 注入：通过 `local.properties` → BuildConfig，不进 Git（等价 iOS 的 Secrets.xcconfig）。

---

## 7. 分阶段计划与产物

| 阶段 | 内容 | 产物 |
|------|------|------|
| P0 | 工程骨架 + 主题色板 + 5 Tab 导航跑通 | 可装的空壳 APK |
| P1 | Agnes 接口层 + 限流 + 数据模型 + 存储 | 接口联调通过 |
| P2 | 拍照/相册 → 识别 → 结果页 → 朗读（核心主流程） | 能演示"拍照学单词" |
| P3 | 绘本生成（后台任务/进度/重试）+ 阅读器 + 自动翻页 | 能演示"生成绘本" |
| P4 | 单词本 / 我的绘本 / 历史 / 首页（含吉祥物、今日任务、头像） | 功能完整 |
| P5 | 视觉打磨、动效、深色模式、加载骨架 | 接近上架质量 |
| P6 | 构建 Release APK、真机安装验证 | **可分发 APK** |

每阶段结束给你一个可安装的 APK + 截图。

---

## 8. 已就绪的本机环境

- ✅ JDK 17（/opt/homebrew/opt/openjdk@17）
- ✅ Android SDK：platform-tools(adb) + platforms;android-34 + build-tools;34.0.0
- ✅ 环境变量已写入 ~/.zshrc
- ⬜ Gradle：建工程时随 wrapper 自动获取
- ⬜ 模拟器镜像（可选，用于我本地验证截图）/ 或直接用你的真机

---

## 9. 待你确认事项

1. 包名：iOS 是 `com.mima.feltwords`，安卓沿用 `com.mima.feltwords` 还是新包名？
2. 应用名：沿用"毛毛英语绘本"？
3. 朗读音色：先用安卓系统 TTS 出 Demo（推荐），还是一开始就接云端 TTS？
4. 演示方式：你用**真机**演示为主（我出 APK 你拷进手机装），需要我额外配模拟器供我本地截图验证吗？
5. WeatherService（天气）是否演示必需？不必需可先略过。
