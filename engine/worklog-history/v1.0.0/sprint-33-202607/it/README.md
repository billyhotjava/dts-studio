# Sprint-33 集成验证（IT）

**状态**: IN_PROGRESS

汇总 Sprint-33 财务可证明正确性的可重跑验证证据。各 Feature 完成时回填，**禁止空占位**（沿用 sprint-30 `it/` 标准）。

本地 contract gate 最近重跑记录：`it/evidence/20260605-local/sprint33-local-it-gates-rerun.md`（全部 PASS；live 接线缺口仍按各项 IN_PROGRESS 保留）。
F2 应用 MySQL oracle 证明链路记录：`it/evidence/20260606-local/f2-application-mysql-oracle-proof.md`（PASS；dbt 包保存在 `dts-copilot/worklog/prs/v1`，不放入 `dts-stack`；runtime HTTP smoke 使用 `it/test_f2_application_mysql_oracle_runtime_http.sh`，配置直连应用 MySQL 后可设置 `REQUIRE_LIVE_APPLICATION_MYSQL_PROOF=true` 强制证明通过）。

## 证据清单（待回填）

| 编号 | 验证项 | 来源 | 重跑方式 | 状态 |
|------|--------|------|----------|------|
| IT-01 | 财务 oracle 注册表 | F1-T01 | `it/test_sprint33_finance_oracle_registry.sh` | PASS（2026-06-05 local） |
| IT-02 | 明细级对账（copilot vs L2 端点逐额） | F1-T02/T03 | `it/test_f1_detail_reconciliation.sh` + `it/test_f1_amount_column_alignment.sh` + `it/evidence/20260605-local/f1-live-oracle-auth-precheck.md` + `it/evidence/20260605-local/f1-live-oracle-admin-session-route-precheck.md` | IN_PROGRESS（core comparator + source orchestration + JSON payload adapter + HTTP provider contract + legacy route 误配提示 + live sample SQL contract + amount tier alignment PASS；admin session auth 已验证可访问 `/api/**`，但当前 `dts-admin` 不承载 legacy `/rs-flowers-base/...` L2 oracle route，需真实 `adminapi/rs-gateway` base URL） |
| IT-03 | 汇总双路对账 | F2-T02 | `it/test_f2_summary_dual_reconciliation.sh` + `it/test_f2_application_mysql_oracle_proof.sh` + `it/test_f2_application_mysql_oracle_runtime_http.sh` + `worklog/prs/v1/tests/test_xycyl_finance_dbt_zip_contract.sh` + `it/evidence/20260606-local/f2-application-mysql-oracle-proof.md` | IN_PROGRESS（summary comparator + case registry + SQL 方言 contract PASS；月对账/售账/2026 凭证 ADS vs 应用 MySQL 原表 oracle SQL contract + 应用内 proof runner/resource + JDBC executor/config + runtime HTTP smoke PASS；v1 dbt zip 已补售账链和月对账 `projectId`；planner 已防 `prs.finance.voucher.profile` 抢走 TPL-57 ADS 模板；直连应用 MySQL JDBC 未配置时 `/prove` 明确 `DISABLED`，live L2/签字 SQL vs copilot dataset 双路取数待接线） |
| IT-04 | 复式凭证 tie-out（借=贷） | F2-T01/T03 | `assets/voucher-tieout-mapping.md` + `it/test_sprint33_voucher_tieout_mapping.sh` + `it/test_f2_voucher_subject_tieout.sh` | IN_PROGRESS（T01 mapping/self-check PASS；T03 subject tie-out contract PASS；live 科目树/真实凭证 tie-out 待签字/接线） |
| IT-05 | 8 条不变量回归（结果级，任意条件） | F3-T01/T02 | `it/test_sprint33_finance_invariant_registry.sh`（T01）；`it/test_f3_invariant_regression.sh`（T02） | PASS（2026-06-05 local；离线代表性网格，live oracle/differential 归 F1/F4） |
| IT-06 | 静态口径 guardrail 拦截（链不混/坏账/去重） | F3-T03 | `it/test_sprint33_static_caliber_guardrails.sh` | PASS（2026-06-05 local；生成层 + analytics 执行前 gate） |
| IT-07 | 差分网格（copilot vs oracle） | F4-T01 | `it/test_f4_differential_grid.sh` | IN_PROGRESS（representative grid registry + comparator contract PASS；live copilot `/api/dataset` vs adminapi/oracle 端点待接线） |
| IT-08 | 持续对账记分卡 + 漂移告警 | F4-T02 | `it/test_sprint33_reconciliation_scorecard.sh` | IN_PROGRESS（scorecard policy + four-lane pass-rate/drift contract + 最新快照 store/source + publisher 完整证据门禁 + schedule provider 骨架 + 签字 baseline 才可压制重复漂移 + actuator `financeReconciliation` health field PASS；live evidence provider 待接线） |
| IT-09 | 财务回答可审计溯源样例 | F5-T01 | `it/test_f5_finance_answer_audit_trail.sh` | IN_PROGRESS（backend audit package + chat/SSE `trace.financeAudit` 自动接线 + scorecard snapshot source/publisher + web TracePanel contract PASS；真实 live scorecard 发布运行、财务回答抽样与审计复跑待补） |
| IT-10 | 财务签字基线 | F5-T02 | `it/test_f5_finance_signoff_baseline.sh` + `assets/finance-signoff-baseline.md` | IN_PROGRESS（engineering evidence package PASS；未签字 baseline 不作为 scorecard 已接受差异；真实财务/审计签字待补，不伪造签字） |
| IT-11 | 弱路径财务 telemetry 入对账候选 | F4-T03 | `it/test_f4_weak_path_reconciliation_candidates.sh` | IN_PROGRESS（weak-path finance candidate + scorecard drift contract PASS；live telemetry/candidate store/Sprint-31 draft flow 待接线） |

## 目录约定

```
it/
  README.md
  evidence/{YYYYMMDD-env}/
  sql/
  test_*.sh
```
