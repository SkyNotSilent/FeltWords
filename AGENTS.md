# Agent 工作约定

本项目是儿童拍照识词绘本 App 的产品、设计和后续开发仓库。后续 Agent 进入仓库时，先阅读：

1. `README.md`
2. `docs/PRD.md`
3. `docs/DESIGN_SYSTEM.md`
4. `docs/FIGMA_DESIGN_BRIEF.md`
5. `docs/DEV_LOG.md`

当前仓库采用双端目录：

- `android/`：当前主要可运行与已验收版本。
- `ios/`：独立 iOS 工程，源码与编译版本，当前未完成完整运行验收。

## 项目方向

- 产品：面向 3-8 岁儿童的英语启蒙 App。
- 核心链路：拍照识物 -> 英文单词 -> 单词本 -> 毛毡小绘本 -> 自动朗读。
- 视觉：软萌儿童 3D 绘本感 + 明亮黄色学习 App + 轻量毛毡手作元素。
- 不要再引入神秘符号、宗教化、惊悚化或深色恐怖元素。

## Git 与版本记录

- 每一个“大方向更新”都要提交 Git，避免只改文件不留版本。
- 大方向更新包括：PRD 调整、视觉方向调整、Figma 链接或设计稿更新、技术架构调整、功能模块实现、关键 bug 修复。
- 提交前确认 `git status --short`，只提交与当前任务相关的文件。
- 提交信息建议使用：
  - `docs: ...`
  - `design: ...`
  - `feat: ...`
  - `fix: ...`
  - `chore: ...`
- 不要重置或回滚用户未要求回滚的改动。

## 开发日志

- `docs/DEV_LOG.md` 是长期开发日志。
- 本仓库使用 `.githooks/pre-commit` 在每次提交前自动追加 staged 文件摘要。
- 如果是重要决策，除了自动摘要，还要手动补充“为什么这样改”和“下一步是什么”。
- 新 clone 后需要执行：

```sh
git config core.hooksPath .githooks
```

## Figma

- 当前 UI 风格稿：https://www.figma.com/design/KAXwdz2TuXBbNLYa87DeQJ
- 修改 Figma 前，优先对照 `docs/DESIGN_SYSTEM.md` 和 `docs/FIGMA_DESIGN_BRIEF.md`。
