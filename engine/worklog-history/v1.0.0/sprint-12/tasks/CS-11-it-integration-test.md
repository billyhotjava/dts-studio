# CS-11: IT 集成测试

**优先级**: P1
**状态**: READY
**依赖**: CS-01~10

## 目标

验证模板快速通道和 SSE 流式输出端到端可用。

## 测试矩阵

| 用例 | 输入 | 预期 | 验证方式 |
|------|------|------|---------|
| IT-01 模板快速通道 | "哪些合同快到期了" | <1s 返回，含 SQL 代码块 | curl /send + 计时 |
| IT-02 非模板同步 | "hi" | 正常返回 JSON | curl /send |
| IT-03 SSE 流式基本 | "hi" | 收到 session → token* → done 事件 | curl /send-stream |
| IT-04 SSE 模板命中 | "哪些合同快到期" | 收到 session → token → done，<1s | curl /send-stream |
| IT-05 SSE 认证 | 无 cookie | 401 | curl /send-stream 无认证 |
| IT-06 降级 | copilot-ai 停止 | 前端降级到同步错误提示 | 停 copilot-ai 后浏览器测试 |
| IT-07 并发 | 2 个用户同时发消息 | 各自收到独立 SSE 流 | 两个 curl 并发 |

## 测试脚本

```bash
# IT-01: 模板快速通道
time curl -s -b /tmp/adm.txt -X POST http://localhost:8092/api/copilot/chat/send \
  -H 'Content-Type: application/json' \
  -d '{"userMessage":"哪些合同快到期了"}'

# IT-03: SSE 流式
curl -s -b /tmp/adm.txt -X POST http://localhost:8092/api/copilot/chat/send-stream \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"userMessage":"hi"}'
```

## 验收标准

- [ ] IT-01~07 全部通过
- [ ] 模板命中场景 < 1s
- [ ] SSE 事件格式符合协议规范
