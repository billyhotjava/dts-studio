# BA-04: AI 屏幕生成对接 copilot-ai

**状态**: READY
**依赖**: BA-01, AE-06

## 目标

将 copilot-analytics 的 AI 屏幕生成功能从调用 dts-platform 改为调用 copilot-ai。

## 技术设计

### 原来的依赖

```java
// PlatformAiNativeClient → dts-platform:/api/ai/analytics/native/screen/generate
// AiConfigClient → dts-ingestion:/api/infra/settings/ai-copilot-analytics
```

### 新方案

```java
// CopilotAiClient → copilot-ai:/api/ai/copilot/screen/generate
// CopilotAiClient → copilot-ai:/api/ai/config/providers (获取 AI 配置)
```

### copilot-ai 新增端点

```
POST /api/ai/copilot/screen/generate  — AI 生成仪表盘布局
POST /api/ai/copilot/screen/revise    — AI 修改仪表盘布局
```

## 影响文件

- `dts-copilot-analytics/src/main/java/.../service/CopilotAiClient.java`（修改：增加屏幕生成调用）
- `dts-copilot-analytics/src/main/java/.../service/ScreenAiGenerationService.java`（修改：替换客户端）
- `dts-copilot-ai/src/main/java/.../web/rest/AiScreenResource.java`（新建）

## 完成标准

- [ ] AI 屏幕生成调用 copilot-ai 成功
- [ ] 生成结果格式与原来一致
- [ ] copilot-ai 不可用时 graceful 降级
