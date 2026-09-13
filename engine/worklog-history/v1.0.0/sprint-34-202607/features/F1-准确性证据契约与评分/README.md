# F1: 准确性证据契约与评分

**优先级**: P0
**状态**: DONE

## 目标

定义统一 `accuracyEvidence` 契约和评分规则，让 NL2SQL 结果从“返回文本”升级为“结果 + 证据 + 可信等级”。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 证据契约与等级定义 | P0 | DONE | - |
| T02 | 评分服务与硬降级规则 | P0 | DONE | T01 |
| T03 | Chat/stream 响应契约接入 | P0 | DONE | T02 |

## 完成标准

- [x] `accuracyEvidence` 字段结构固定并有 contract test。
- [x] HIGH/MEDIUM/LOW/UNTRUSTED 可机器判定。
- [x] 所有 NL2SQL 完成事件都能带回证据摘要。
