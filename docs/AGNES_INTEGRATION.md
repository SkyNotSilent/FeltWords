# Agnes API 接入说明

日期：2026-06-05

## 已确认接口

- Base URL：`https://apihub.agnes-ai.com/v1`
- 鉴权：`Authorization: Bearer <API_KEY>`
- 图片识别与故事生成：`POST /chat/completions`
- 毛毡插图生成：`POST /images/generations`

当前 API Key 可用模型通过 `GET /models` 实测为：

- `agnes-2.0-flash`
- `agnes-1.5-flash`
- `agnes-image-2.1-flash`
- `agnes-image-2.0-flash`
- `agnes-video-v2.0`

注意：官网文档 URL 使用 `agnes-20-flash` / `agnes-image-21-flash`，但实际请求中的模型 ID 带小数点。

## App 调用链路

1. 相机拍照后，在设备端缩放到最长边 1200px 并压缩 JPEG。
2. 以 OpenAI 兼容的 `image_url` data URL 发送给 `agnes-2.0-flash`。
3. 模型返回严格 JSON：英文单词、中文解释、置信度、分类、例句、视觉描述与备选词。
4. 优先把原始照片作为 `img2img` 输入调用 `agnes-image-2.1-flash`，生成统一毛毡绘本风格插图；若服务端不接受 data URL，则自动退回到基于视觉描述的文生图。
5. 使用识别单词调用 `agnes-2.0-flash`，生成四页低龄英文故事。
6. 使用 iOS `AVSpeechSynthesizer` 免费朗读单词和故事句子。

## 品牌 IP 静态资产

- 首页与空状态使用 Agnes `agnes-image-2.1-flash` 预生成的本地静态资产，不在用户打开页面时实时生成。
- 先生成女孩、男孩与小熊毛毛的统一角色母图，再以母图作为 img2img 参考派生快捷卡片和空状态，减少角色跨页面变形。
- 角色保持玩偶化 3D 毛毡造型，不使用真实儿童面孔；历史场景仅展示物品记忆卡。
- 生成原图审核后缩放至移动端资源尺寸并随 App 打包，避免运行时等待、生成成本与内容安全波动。
- 首页每日主题场景沿用同一角色母图做 img2img，首批包含好好吃饭、快乐学习、一起玩耍、整理物品和睡前准备五个主题。
- 每日主题场景图不直接生成文字，正向引导文案由 SwiftUI 叠加，保证中文准确并便于后续调整。

## 限流与密钥

- 客户端使用滑动时间窗限流器，最多 20 次/分钟，达到上限会等待后再请求。
- 上传前使用 Apple Vision 在设备端检测人脸；检测到人物照片时阻止上传，引导只拍物品。
- iOS API Key 仅存放在本地 `ios/Config/Secrets.xcconfig`；Android API Key 仅存放在本地 `android/local.properties`。两个文件均已加入 `.gitignore`。
- 正式上线时不应在客户端保存供应商 API Key，应改为通过自有后端代理并增加用户级配额、审计与内容安全策略。
- Agnes 当前隐私政策注明服务不面向 13 岁以下儿童。面向儿童正式发布前，必须与供应商确认儿童数据处理条款，并落实可验证的家长同意、数据删除和跨境传输合规方案。
