# Sprint-33 local IT gates rerun

**时间**: 2026-06-05
**环境**: local repo (`dts-copilot`)
**结论**: PASS。Sprint-33 现有本地 contract / IT 脚本全部重跑通过；F1 live 双路取数分离认证改动后已二次复跑；F4-T02 记分卡健康字段、scorecard publisher 完整证据门禁与 schedule provider 骨架已纳入 `test_sprint33_reconciliation_scorecard.sh`；F5-T02 已补充“未签字 baseline 不可压制 scorecard 漂移”的 TDD 门禁。剩余 IN_PROGRESS 项仍按任务文件记录为 live oracle、真实端点、live evidence provider 或财务/审计签字接线缺口。

## 重跑脚本

```text
PASS worklog/v1.0.0/sprint-33-202607/it/test_f1_amount_column_alignment.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f1_detail_reconciliation.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f2_summary_dual_reconciliation.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f2_voucher_subject_tieout.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f3_invariant_regression.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f4_differential_grid.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f4_weak_path_reconciliation_candidates.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_answer_audit_trail.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_signoff_baseline.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_sprint33_finance_invariant_registry.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_sprint33_finance_oracle_registry.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_sprint33_reconciliation_scorecard.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_sprint33_static_caliber_guardrails.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_sprint33_voucher_tieout_mapping.sh
```

## 2026-06-05 补充验证

```text
PASS mvn -q -pl dts-copilot-ai -Dtest=FinanceSignoffBaselineServiceTest test
PASS mvn -q -pl dts-copilot-ai -Dtest=FinanceDetailReconciliationHttpPayloadProviderTest test
PASS worklog/v1.0.0/sprint-33-202607/it/test_f1_detail_reconciliation.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_signoff_baseline.sh
PASS worklog/v1.0.0/sprint-33-202607/it/test_sprint33_reconciliation_scorecard.sh
PASS mvn -q -pl dts-copilot-ai -DskipTests package
PASS mvn -q -pl dts-copilot-ai -Dtest=FinanceChatAuditTrailServiceTest,AgentExecutionServiceTest,AgentChatServiceTest test
PASS mvn -q -pl dts-copilot-ai -Dtest=FinanceReconciliationScorecardSnapshotServiceTest test
PASS mvn -q -pl dts-copilot-ai -Dtest=FinanceReconciliationScorecardPublisherServiceTest test
PASS mvn -q -pl dts-copilot-ai -Dtest=FinanceReconciliationScorecardScheduledPublisherServiceTest test
PASS worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_answer_audit_trail.sh
```

F1 补充规则：当 live oracle `/rs-flowers-base/...` 返回非 2xx 时，HTTP provider 的异常会直接提示 `copilot.finance.reconciliation.oracle-base-url` 需要 legacy `adminapi` / `rs-gateway` / `rs-flowers-base` 入口，可使用 `/flowers-dev-api` 或等价 gateway base URL，不要指向当前 `dts-admin /api` 服务。

补充规则：`PENDING_SIGNATURE` 的财务签字基线不会向 scorecard 提供已接受差异，不能吞掉新增漂移；只有 `SIGNED` 且 `accepted=true` 的基线才可压制重复漂移告警。`FinanceSignoffBaselineService` 生成的 baseline markdown 也会明确输出“漂移基线采信”状态，避免重新生成资产时丢失业务说明。

F5-T01 补充规则：财务 chat 计划命中报表 code、oracle binding 或 ADS 模型时，SSE `done` metadata 与持久化 assistant message trace 会自动带出 `trace.financeAudit`；`FinanceReconciliationScorecardSource` 有对应 oracle binding 的最新 scorecard 时，`oracleStatus` 反映真实 `PASS/DRIFT/FAIL`；source 缺失或无结果时保持 `MISSING_SCORECARD`，不会伪造成对账 PASS。

F4/F5 补充规则：`FinanceReconciliationScorecardSnapshotService` 可将 scorecard 按 oracle binding 持久化为最新快照，并作为 `FinanceReconciliationScorecardSource` 被 F5 审计包读取；坏 JSON/不可读快照返回 empty，不能伪造健康状态。

F4/F5 publisher 补充规则：`FinanceReconciliationScorecardPublisherService` 只有在 F1/F2/F3/F4 required lane 齐全时才发布最新 snapshot；缺 lane 时返回 `PENDING_LIVE_EVIDENCE` 且不落库，避免缺 live 证据时覆盖 F5 审计 source。

F4 schedule 补充规则：`FinanceReconciliationScorecardScheduledPublisherService` 只消费已注册的 `FinanceReconciliationScorecardEvidenceProvider`；无 provider 时 `SKIPPED`，有 provider 时逐个交给 publisher，不在调度层造财务报告。

## 保留缺口

- F1-T02: live L2 adminapi oracle + copilot dataset 双路取数待接线。
- F2-T02/T03: 真实 L2/签字 SQL、真实凭证科目树与 L3 tie-out 待接线/确认。
- F4-T01/T02/T03: live 差分网格、live evidence provider、真实 baseline 采信、真实 route telemetry 候选持久化待接线；F4-T02 health field 已在 `/actuator/health` 暴露 `financeReconciliation=UP`，不替代 live evidence。

## 运行态补充

```text
GET http://127.0.0.1:50091/actuator/health
status=UP
components.financeReconciliation.status=UP
```
- F5-T01/T02: 真实 live scorecard 发布运行、真实财务回答抽样/审计复跑、真实财务/审计签字待完成。
