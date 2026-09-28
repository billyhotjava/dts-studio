# Sprint-34 IT 验证计划

## 验证矩阵

| Feature | 验证项 | 命令/证据 | 状态 |
|---------|--------|-----------|------|
| F1 | `accuracyEvidence` 契约字段和等级枚举 | `mvn -pl dts-copilot-ai -Dtest='*Accuracy*,CopilotChatRequestContextTest,AgentChatServiceTest,AgentExecutionServiceTest' test` | PASS |
| F2 | L0/profile/MySQL 直连硬降级 | `Nl2SqlAccuracyEvidenceContractTest` | PASS |
| F3 | ODS/dbt freshness 与 tie-out evidence | `it/test_sprint34_accuracy_plan.sh`；snapshot freshness contract 和 live dbt diagnostics probe 已 PASS | PASS |
| F4 | 黄金集和形变测试 scorecard | `Nl2SqlAccuracyGoldenSetRegistryTest` + `Nl2SqlAccuracyGoldenSetReleaseEvidenceServiceTest`；覆盖黄金集、L0/profile、应用 MySQL、形变漂移和 release evidence 门禁 | PASS |
| F5 | 前端可信解释和低可信降级 | `pnpm exec vitest run src/api/modules/copilotStreamEvent.test.ts src/api/aiChatCompatibility.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/TracePanel.test.tsx src/components/copilot/MessageList.platformIndicator.test.tsx` | PASS |

## 首批黄金问题

| 问题 | 期望路线 | 最低等级 |
|------|----------|----------|
| `2026年凭证的数据统计下` | `TPL-57` -> `public.xycyl_ads_finance_voucher_monthly` | HIGH |
| `2026年财务数据统计下` | `TPL-56` -> finance ADS | MEDIUM |
| `本月报花订单明细看下` | `TPL-FLOWERBIZ-ORDER-MONTHLY` -> `public.xycyl_dws_flowerbiz_order_monthly` | MEDIUM |
| `随便查一下财务` | clarification 或 LOW | LOW |

## DONE 门禁

- [x] 每个 P0 case 有 route、SQL、数据层、freshness、tie-out 或降级原因。
- [x] 任一 P0 case 回落 L0 profile 时门禁失败。
- [x] 任一 P0 case 直连应用 MySQL 并标高可信时门禁失败。
- [x] 证据包不包含 JDBC 密码、token、明文 secret。

## 2026-06-07 本地验证

- `mvn -q -pl dts-copilot-ai -Dtest='*Accuracy*,PlatformDbtFreshnessClientTest,CopilotChatRequestContextTest,AgentChatServiceTest,AgentExecutionServiceTest,Nl2SqlAccuracyGoldenSetReleaseEvidenceServiceTest' test`
- `pnpm exec vitest run src/api/modules/copilotStreamEvent.test.ts src/api/aiChatCompatibility.test.ts src/components/copilot/copilotStreamReducer.test.ts src/components/copilot/TracePanel.test.tsx src/components/copilot/MessageList.platformIndicator.test.tsx`
- `pnpm run typecheck`
- `bash worklog/v1.0.0/sprint-34-202607/it/test_sprint34_accuracy_plan.sh`
- 补充回归：同步/流式非只读候选 SQL 均只进入 `accuracyEvidence` 评分并降为 `UNTRUSTED`，不会进入可执行 `generatedSql`。
- 补充回归：`freshness` snapshot 支持 `FRESH` / `STALE` / `MISSING` / `UNKNOWN` 归一化；`MISSING` 降为 `UNTRUSTED`，`STALE` 将已对账 ADS/DWS 结果上限压到 `MEDIUM`。
- 补充回归：无请求侧 snapshot 时，`PlatformDbtFreshnessClient` 通过 dts-platform dbt diagnostics 解析 `FRESH` / `STALE` / `MISSING`，并由 `AgentExecutionService` 合并到流式 done contract。
- 补充回归：`Nl2SqlAccuracyGoldenSetRegistryTest` 加载 `nl2sql-accuracy-golden-set.v1.json`，并用 scorecard 断言黄金集不能回落 L0 profile、应用 MySQL 或漂移到错误模板/target。
- 补充回归：`Nl2SqlAccuracyGoldenSetReleaseEvidenceServiceTest` 断言 scorecard 能输出 `generatedAt`、release gate、失败摘要和弱路径来源；缺 scorecard 时返回 `MISSING_SCORECARD`，不伪造 PASS。
- 补充回归：`MessageList.platformIndicator.test.tsx` 断言 UNTRUSTED/L0 profile 不展示“来自平台指标”，改为“可信度不足 / 不作为统计结论 / 先执行入湖或 dbt 构建”的可操作提示。
- 补充回归：财务审计状态使用 `authorityStatus.authorityLevel` 作为新契约，stream done event 与 legacy chat response 均兼容旧 `oracleStatus` / `oracleLevel`，避免用户把权威基准误解为 Oracle 数据库。
