# FE-03: API 客户端适配

**状态**: READY
**依赖**: FE-01, FE-02

## 目标

修改前端 API 客户端，指向 copilot-ai (8091) 和 copilot-analytics (8092) 后端。

## 技术设计

### API 基础 URL 配置

```typescript
// src/config/api.ts
export const API_CONFIG = {
  copilotAi: import.meta.env.VITE_COPILOT_AI_URL || 'http://localhost:8091',
  copilotAnalytics: import.meta.env.VITE_COPILOT_ANALYTICS_URL || 'http://localhost:8092',
}
```

### 认证头注入

```typescript
// 所有请求自动添加 API Key
const apiClient = axios.create({
  headers: {
    'Authorization': `Bearer ${getApiKey()}`,
    'X-DTS-User-Id': getCurrentUserId(),
    'X-DTS-User-Name': getCurrentUserName(),
  }
})
```

### API Key 来源

前端部署时通过环境变量或运行时配置注入 API Key。
iframe 嵌入模式下，API Key 通过 URL 参数或 postMessage 传入。

### 环境变量

```env
VITE_COPILOT_AI_URL=http://localhost:8091
VITE_COPILOT_ANALYTICS_URL=http://localhost:8092
VITE_API_KEY=cpk_xxx
```

## 影响文件

- `dts-copilot-webapp/src/api/analyticsApi.ts`（修改：base URL）
- `dts-copilot-webapp/src/api/copilotAiApi.ts`（修改：base URL）
- `dts-copilot-webapp/src/config/api.ts`（新建）
- `dts-copilot-webapp/.env.development`（新建）

## 完成标准

- [ ] BI API 调用正确指向 copilot-analytics
- [ ] AI API 调用正确指向 copilot-ai
- [ ] 认证头正确注入
- [ ] 环境变量可配置
