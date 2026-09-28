# Sprint-6: 前端 Webapp 抽取与整合 (FE)

**前缀**: FE (Frontend)
**状态**: READY
**目标**: 从 dts-analytics-webapp fork 前端代码，合并 dts-platform-webapp 的 AI 聊天组件，适配 API 客户端指向 copilot 后端，支持 iframe 嵌入模式。

## 背景

dts-copilot-webapp 由两个前端项目合并而来：
- **dts-analytics-webapp**: BI 界面（仪表盘、查询、屏幕设计器、报表）
- **dts-platform-webapp 的 AI 组件**: Copilot 聊天面板、Tool 调用卡片、审批卡片

### 技术栈

- React 19 + TypeScript
- Vite 6
- Ant Design 5
- ECharts 6
- React Router 7
- pnpm

## 任务列表

| ID | 任务 | 状态 | 依赖 |
|----|------|------|------|
| FE-01 | dts-analytics-webapp fork 与品牌重构 | READY | BA-01 |
| FE-02 | AI 聊天面板组件合并（从 dts-platform-webapp） | READY | FE-01 |
| FE-03 | API 客户端适配（指向 copilot-ai + copilot-analytics） | READY | FE-01, FE-02 |
| FE-04 | iframe 嵌入模式支持 | READY | FE-03 |
| FE-05 | Dockerfile 与静态资源打包 | READY | FE-01~04 |
| FE-06 | 前端集成测试 | READY | FE-01~05 |

## 完成标准

- [ ] `pnpm build` 构建成功
- [ ] BI 界面所有页面可访问
- [ ] AI 聊天面板可发送消息并接收流式响应
- [ ] iframe 嵌入模式正常工作（无跨域、布局正确）
- [ ] Docker 镜像构建成功并可通过 Nginx 访问

## IT 验证命令

```bash
cd dts-copilot/dts-copilot-webapp && pnpm install && pnpm build

# 开发模式
cd dts-copilot/dts-copilot-webapp && pnpm dev  # http://localhost:3003

# Docker 构建
docker build -t dts-copilot-webapp ./dts-copilot-webapp
```

## 优先级说明

FE-01 → FE-02 → FE-03 顺序 → FE-04 → FE-05 → FE-06 收尾
