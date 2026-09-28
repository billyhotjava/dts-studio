# T03: SQL 静态口径 guardrail

**优先级**: P1
**状态**: DONE
**依赖**: T02

## 目标

在执行前识别明显不可信 SQL：非只读、未绑定参数、跨链混 SUM、错误 join key、禁用表。

## 技术设计

- 复用已有 SQL guardrail 和 Sprint-31 口径规则。
- 输出 `staticChecks`：`READ_ONLY`、`ALLOWED_RELATION`、`PARAM_BOUND`、`CALIBER_RULE_PASS`。
- 不通过时 evidence 降为 `UNTRUSTED`，并返回可读原因。

## 影响范围

- NL2SQL SQL validator。
- finance/procurement/domain guardrail tests。

## 验证

- [x] 非 SELECT 被拒绝。
- [x] 未登记表被降级。
- [x] 财务两链混算被 guardrail 拦截。

## 完成标准

- [x] SQL 静态检查结果进入 evidence，并参与评分。
