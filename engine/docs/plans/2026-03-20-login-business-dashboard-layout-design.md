# Login NL2SQL Showcase Design

**Date:** 2026-03-20

## Goal

把 `dts-copilot` 登录页从简单表单卡片重构为 `DTS 智能数据分析助手` 的产品封面：顶部使用中文主标题与副标题，左侧突出 `NL2SQL` 转换链，右侧保留登录面板。

## Layout

- 顶部主标题：`DTS 智能数据分析助手`
- 顶部副标题：`AI-Native 智能数据分析平台`
- 中部主区域左右分栏
- 左侧为 `NL2SQL` 品牌演示封面
- 右侧为登录面板
- 左下角保留一句简短能力口号

## Left Showcase

左侧不再使用任何容易被理解为实时监控的图表，而改成 `NL2SQL` 工具封面：

- 一条清晰的转换链：
  - 自然语言提问
  - Schema 感知与 SQL 安全校验
  - 生成 SQL
  - 结果表格 / 图表输出
- 视觉上用发光卡片、代码块、结果示意和细连线表达“从问题到结果”的过程
- 下方使用三张能力卡：
  - NL2SQL 智能生成
  - 多数据源分析
  - AI 对话与可视化

这样既保留科技感，也能准确表达产品是一个 `AI-Native 智能数据分析平台`，而不是某个具体行业的经营系统。

## Right Panel

- 保留当前登录逻辑
- 继续使用用户名、密码、登录按钮
- 文案切换为 `DTS 智能数据分析助手`
- 面板视觉与左侧封面同属一个深色体系，但信息密度更克制

## Styling

- 浏览器目标：Chrome 108+
- 整体为深色、科技感、偏 AI 数据分析产品
- 顶部主标题字号需要明显放大
- 登录区不做弹窗式强悬浮，而是内嵌式侧栏

## Files

- `dts-copilot-webapp/src/pages/auth/LoginPage.tsx`
- `dts-copilot-webapp/src/pages/auth/auth.css`
- `dts-copilot-webapp/tests/loginPageLayout.test.ts`

## Verification

- 登录页 `NL2SQL` 主题结构测试通过
- `tsc --noEmit` 通过
- `vite build` 通过
