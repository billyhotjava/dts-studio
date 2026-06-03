# T02: 9 铁律违规 SQL 拦截回归

**优先级**: P1
**状态**: READY
**依赖**: F1-T03

## 目标

为 9 条口径铁律的可静态判定项建立"反例必拦、正例必放"的回归集，扩展自 sprint-30 F3 的 `SemanticPackCaliberGuardrailTest`，并以 F1-T03 机器规则为输入。

## 技术设计

- 每条可静态判定铁律 ≥1 正例 + ≥1 反例 SQL：
  - 两条结算链混 SUM（a_month_accounting + a_sale_account 同聚合）→ 拦
  - biz_type 无表限定过滤 → 拦
  - JSON 列出现在 = JOIN → 拦
  - a_sale_account_rent_item.rent 未 CAST → 拦
  - 月对账三级金额选错列（结合 fewShots 提示，难静态者降级为标注校验）
- 回归直接吃 F1-T03 的 `caliber-rules`，确保"规则即测试输入"，规则改 → 回归同步。
- 与 F4-T01 共用 CI gate。

## 影响范围

- `dts-copilot-ai` 测试（扩展 SemanticPackCaliberGuardrailTest 或新增）
- 关联 sprint-30 F3 guardrail 校验逻辑

## 验证

- [ ] 9 条铁律可静态项的反例全部被拦、正例全部放行
- [ ] 新增/修改规则后回归自动覆盖

## 完成标准

- [ ] 铁律违规拦截回归绿且与规则同源
