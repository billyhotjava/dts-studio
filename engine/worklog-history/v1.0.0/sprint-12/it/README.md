# Sprint-12 IT 测试

## 测试范围

| 用例 | 场景 | 预期结果 |
|------|------|---------|
| IT-01 | 模板快速通道（同步端点） | <1s，含 SQL 代码块 |
| IT-02 | 非模板同步 | 正常 JSON 响应 |
| IT-03 | SSE 流式基本 | session → token* → done |
| IT-04 | SSE 模板命中 | session → token → done, <1s |
| IT-05 | SSE 认证 | 401 |
| IT-06 | 降级 | 前端降级到同步 |
| IT-07 | 并发 | 独立 SSE 流 |

## 前置条件

- copilot-ai 运行中（8091）
- copilot-analytics 运行中（8092）
- 已登录获取 session cookie

## 可执行 Smoke

当前目录新增了可执行脚本：

```bash
./test_streaming_guardrails.sh
```

脚本默认直接验证 `copilot-ai` 内部 SSE 端点，避免依赖 analytics 登录态，覆盖这轮修复后的两条关键保护：

- SSE 基本事件序列：`session -> token* -> done`
- 流式端点会话归属校验：错误用户使用他人 `sessionId` 时返回 `404`

可选环境变量：

```bash
AI_BASE_URL=http://127.0.0.1:8091
COPILOT_ADMIN_SECRET=change-me-in-production
```
