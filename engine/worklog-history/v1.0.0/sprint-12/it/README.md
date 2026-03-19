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
