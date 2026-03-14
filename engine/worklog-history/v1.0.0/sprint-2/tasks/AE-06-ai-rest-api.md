# AE-06: AI REST API 端点

**状态**: READY
**依赖**: AE-04, AE-05

## 目标

创建 copilot-ai 的 AI REST API 端点，对外暴露 Copilot 能力。

## 技术设计

### 端点设计

```
POST /api/ai/copilot/complete       — SQL 补全
POST /api/ai/copilot/stream         — 流式补全（SSE）
POST /api/ai/copilot/nl2sql         — 自然语言转 SQL
POST /api/ai/copilot/explain        — SQL 解释
POST /api/ai/copilot/optimize       — SQL 优化
GET  /api/ai/copilot/status         — AI 可用性状态

GET  /api/ai/config/providers       — Provider 列表
POST /api/ai/config/providers       — 新增 Provider
PUT  /api/ai/config/providers/{id}  — 修改 Provider
DELETE /api/ai/config/providers/{id} — 删除 Provider
POST /api/ai/config/providers/{id}/test — 测试连通性
```

### 认证

此阶段暂不加认证（sprint-4 加入 API Key）。预留 `@ApiKeyRequired` 注解位。

### 响应格式

```json
{
  "success": true,
  "data": { "sql": "SELECT ...", "model": "qwen2.5-coder:7b", "duration_ms": 1234 },
  "error": null
}
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiCopilotResource.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/AiConfigResource.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/web/rest/dto/ApiResponse.java`（新建）

## 完成标准

- [ ] 所有端点可通过 curl 调用
- [ ] SSE 流式端点正确返回 `text/event-stream`
- [ ] 错误情况返回统一的错误格式
- [ ] Swagger/OpenAPI 文档自动生成
