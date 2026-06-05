# Sprint-33 财务签字基线

**账期**: 2026-06
**Scorecard**: sprint33-finance-daily-scorecard / PASS / passRate=100.00 / maxDifference=0.00
**签字状态**: PENDING_SIGNATURE
**漂移基线采信**: 未采信。当前仅代表工程证据包已成文，缺少财务/审计签字时不得作为 scorecard 已接受差异基线。

## IT 证据

| Feature | 证据 | 状态 | 重跑命令 | 日志 |
|---------|------|------|----------|------|
| F1 | 明细级对账 | PASS | `mvn -q -pl dts-copilot-ai -Dtest=FinanceDetailReconciliationServiceTest test` | `worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f1-detail-reconciliation-live-sample-sql-test.log` |
| F2 | 汇总双路对账 | PASS | `mvn -q -pl dts-copilot-ai -Dtest=FinanceSummaryDualReconciliationServiceTest test` | `worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f2-summary-dual-reconciliation-test.log` |
| F2 | 凭证科目 tie-out | PASS | `mvn -q -pl dts-copilot-ai -Dtest=FinanceVoucherSubjectTieoutServiceTest test` | `worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f2-voucher-subject-tieout-test.log` |
| F3 | 不变量与静态口径 guardrail | PASS | `mvn -q -pl dts-copilot-ai -Dtest=FinanceInvariantRegressionServiceTest,CaliberRuleRegistryTest test` | `worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/static-caliber-guardrails-test.log` |
| F4 | 差分网格与持续记分卡 | PASS | `mvn -q -pl dts-copilot-ai -Dtest=FinanceDifferentialGridServiceTest,FinanceReconciliationScorecardServiceTest test` | `worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f4-reconciliation-scorecard-test.log` |
| F5 | 可审计溯源样例 | PASS | `bash worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_answer_audit_trail.sh` | `worklog/v1.0.0/sprint-33-202607/it/evidence/20260605-local/f5-finance-answer-audit-trail-test.log` |

## 签字

| 角色 | 名称 | 时间 | 备注 |
|------|------|------|------|
| 财务负责人 |  |  | 待财务负责人复核后填写 |
| 审计复核 |  |  | 待审计复核后填写 |

## 未完成项

- Finance signoff baseline pending: missing required signature=FINANCE_OWNER,AUDITOR
- 只有签字状态达到 SIGNED 后，已登记差异才可压制后续重复漂移告警；当前 PENDING_SIGNATURE 不压制漂移。
