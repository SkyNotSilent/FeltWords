# 开发日志

本文件用于保存产品、设计和开发阶段的重要版本记录。普通提交会由 Git hook 自动追加 staged 文件摘要；重要方向变更需要手动补充背景和决策。

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
