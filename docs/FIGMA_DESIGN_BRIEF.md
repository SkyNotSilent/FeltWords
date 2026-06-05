# 设计文件说明 / Figma 出稿 Brief

版本：v0.1  
日期：2026-06-05  
用途：交给设计师或用于 Figma 初稿生成

## 1. 项目一句话

设计一款面向 3-8 岁儿童的 iOS 英语启蒙 App。孩子通过拍照识别身边物体，学习英文单词，并把单词生成软萌连环画绘本和自动朗读小视频。

## 2. 视觉方向

整体风格：

软萌 3D 儿童绘本 + 黄色学习任务 App + 浅层毛毡手作元素。

参考拆解：

- 拍照页参考第一张图：全屏相机、中央取景框、底部大快门、左右辅助按钮。
- 布局和学习体系参考第二张图：黄色主色、任务卡、路线式进度、可爱 mascot、学习任务入口。
- 角色和氛围参考第三张图：软萌 3D 儿童角色、浅蓝天空、云朵、圆润控件。

毛毡元素表达：

不是神秘符号化装饰，而是“儿童手作绘本质感”。使用羊毛纹理、毛毡贴片、缝线边框、纽扣星星、布艺云朵、软布小花、贴布印章。

## 3. Frame 列表

### 3.1 Onboarding / Age Select

尺寸：393 x 852

画面：

- 背景为浅蓝天空和奶油云朵。
- 中央 mascot 坐在云朵上，手里拿毛毡贴布书签。
- 标题：拍一拍，变成英文小绘本
- 副标题：用身边的东西学英语
- 年龄选择：3-4 岁、5-6 岁、7-8 岁
- 主按钮：开始

### 3.2 Home

尺寸：393 x 852

布局参考第二张图：

- 顶部：Hi，小悠好～
- 右上：头像
- 背景：明亮黄色，带低透明度毛毡纹理和学习小图标。
- 中央：大 mascot，戴圆眼镜，挥手。
- 中下：今日任务白色卡片。
- 今日任务内容：
  - 拍照找 1 个英文
  - 听 3 次发音
  - 看 1 本小绘本
- 主按钮：开始拍照
- 主按钮区域使用横向分页页面卡片，固定顺序为“开始拍照、我的绘本、单词本、历史记录”；默认显示开始拍照并露出下一张卡，卡片显示页码、滑动提示和实时内容摘要。
- 底部导航：主页、拍一拍、我的绘本、单词本、历史记录；历史记录位于单词本右侧。

### 3.3 Camera

尺寸：393 x 852

布局参考第一张图：

- 全屏相机预览。
- 中央标准正方形取景框，只显示四个白色圆角角标。
- 取景框必须在屏幕内居中，四个角不要切入被识别物体主体。
- 取景框下方文案：把物品放进小框里
- 底部中央大快门，白色外圈，橙色内圈。
- 左下：关闭按钮。
- 右下：相册按钮。
- 顶部不要复杂工具栏。
- 顶部不要画刘海屏、状态条、横向胶囊条或“识别物体”标题。
- 不要在取景框上加入斜线、箭头、识别标签或复杂装饰。

状态变体：

- 默认取景
- 对焦中
- 识别中
- 识别失败

### 3.4 Recognition Result

尺寸：393 x 852

画面：

- 背景：Cloud Cream。
- 顶部返回。
- 上方照片缩略图，圆角 28。
- 中央英文单词大标题：apple
- 单词旁边发音喇叭贴布按钮。
- 中文辅助：苹果
- 例句卡：I see a red apple.
- 备选词 chips：apple、fruit、snack
- 主按钮：生成小绘本
- 次按钮：加入单词本

视觉重点：

- 英文单词必须是第一视觉焦点。
- 生成小绘本按钮使用黄色。

### 3.5 Story Generating

尺寸：393 x 852

画面：

- 背景浅黄色。
- mascot 拿着画笔和毛毡贴片。
- 文案：毛毛正在画第 2 页
- 四个进度点：1、2、3、4
- 下方小卡片展示来源词：apple

动效建议：

- 毛毡贴片轻轻飘动。
- 进度点逐个盖章。

### 3.6 Story Reader

尺寸：393 x 852

画面：

- 顶部：返回、标题、收藏。
- 中央大插图，占屏幕 65%。
- 插图风格：软萌 3D 绘本，浅蓝天空，温暖黄色点缀，毛毡贴片/缝线/布艺云朵作为场景装饰。
- 底部白色句子卡。
- 英文句子：Ben sees an apple.
- 当前重点词 apple 用黄色高亮。
- 控制区：上一页、播放/暂停、下一页。
- 页码点：1/4。

### 3.7 Story Library

尺寸：393 x 852

画面：

- 标题：我的绘本
- 顶部横向筛选：全部、食物、动物、玩具、自然
- 双列卡片
- 卡片内容：封面、标题、来源词、播放进度
- 空状态：mascot 坐在布艺小屋前，文案：还没有绘本，去拍一个吧

### 3.8 Wordbook

尺寸：393 x 852

画面：

- 标题：单词本
- 顶部统计卡：已经认识 12 个英文
- 分类 tab：全部、食物、动物、玩具、家里
- 单词卡网格
- 单词卡：图片、英文、中文、小发音按钮、掌握印章

### 3.9 Word Detail

尺寸：393 x 852

画面：

- 大图
- 英文单词
- 发音按钮
- 中文辅助
- 例句
- 最近出现于哪本绘本
- 按钮：再听一遍、生成新故事、我记住了

### 3.10 Practice

尺寸：393 x 852

画面：

- 题型：看图选词
- 上方图片卡
- 问题：Which word is it?
- 选项卡：apple / ball / cat
- 反馈：小印章 + 星点
- 进度：1/3

### 3.11 Parent Center

尺寸：393 x 852

画面：

- 标题：家长中心
- 儿童档案
- 年龄段选择
- 隐私设置
- 是否保存原始照片
- 是否允许云端识别
- 每日使用提醒
- 删除本地数据

风格：

- 保留暖色，但布局更清晰克制。
- 不使用大面积动画。

### 3.12 Recognition History

尺寸：393 x 852

画面：

- 标题：历史记录
- 按识别时间倒序排列卡片。
- 卡片内容：识别图片、英文、中文、存入时间、发音。
- 卡片操作：存入单词本、生成绘本。
- 空状态文案：拍照识别后会自动保存在这里。

## 4. 组件清单

需要设计的组件：

- App Tab Bar
- Primary Button
- Secondary Button
- Icon Button
- Camera Shutter
- Camera Focus Frame
- Word Card
- Story Card
- Story Page Sentence Card
- Progress Path
- Task Card
- Category Chip
- Parent Setting Row
- Empty State
- Loading State
- Success Stamp

## 5. Mascot 设计要求

角色名暂定：毛毛

方向：

- 可爱、圆润、3D。
- 可以是小熊、小朋友或小绒球。
- 戴圆眼镜，与第二张图的学习陪伴感一致。
- 有毛毡小背包、贴布书签、黄色披肩。

表情：

- 开心挥手
- 认真识别
- 画绘本
- 鼓励
- 睡觉/休息

禁止：

- 尖锐配饰
- 脏污破损材质
- 武器
- 阴森表情

## 6. 插图风格 Prompt

通用插图风格：

```text
Soft 3D children's picture book illustration, rounded cute characters, warm sunshine yellow accents, pastel sky blue background, fluffy clouds, cozy felt craft texture, wool fibers, stitched edges, felt patches, button stars, soft fabric flowers, friendly and safe for preschool children, no scary elements, no dirty or broken texture, no realistic child face, high quality, clean composition.
```

物体绘本示例：

```text
A red apple on a soft cloud-like table, a cute felt mascot wearing round glasses points at it, warm yellow light, stitched felt patch decoration, button stars, soft 3D children's picture book style, safe and cheerful.
```

## 7. 关键页面文案

首页：

- Hi，小悠好～
- 今天想认识什么英文？
- 拍一拍，找英文

拍照页：

- 把物品放进小框里
- 毛毛在看
- 找英文中
- 没有看清，再拍一次

识别结果：

- 找到啦，是 apple
- 跟我读
- 生成小绘本
- 加入单词本

绘本生成：

- 毛毛正在画第 2 页
- 故事快好啦

绘本结束：

- 今天认识了 apple
- 再听一遍
- 去单词本看看

## 8. 设计验收标准

- 首页一眼能看出是儿童英语学习产品。
- 拍照页和第一张参考图结构一致：全屏相机、取景框、大快门。
- 整体布局和第二张参考图一致：黄色学习感、卡片、任务进度、mascot 陪伴。
- 视觉元素和第三张参考图一致：软萌 3D、云朵、圆润按钮、儿童友好。
- 毛毡元素轻量、柔软、有手作质感。
- 儿童主操作按钮足够大。
- 每页主行动明确，不出现复杂设置。
- 英文单词和绘本句子具有最高阅读优先级。
