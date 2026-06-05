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

## 限流与密钥

- 客户端使用滑动时间窗限流器，最多 20 次/分钟，达到上限会等待后再请求。
- API Key 仅存放在本地 `Config/Secrets.xcconfig`，该文件已加入 `.gitignore`。
- 正式上线时不应在客户端保存供应商 API Key，应改为通过自有后端代理并增加用户级配额、审计与内容安全策略。
