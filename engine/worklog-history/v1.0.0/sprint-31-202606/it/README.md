# Sprint-31 集成验证（IT）

**状态**: IN_PROGRESS

本目录汇总 Sprint-31 语义口径收口的可重跑验证证据。证据由各 Feature 完成时回填，**禁止空占位**（沿用 sprint-30 `it/` 标准）。

## 证据清单（待回填）

| 编号 | 验证项 | 来源 | 重跑方式 | 状态 |
|------|--------|------|----------|------|
| IT-01 | 三源口径差异矩阵 | F1-T01 | `assets/caliber-source-diff-matrix.md` | TODO |
| IT-02 | 定源 ADR 签署 | F1-T04 | `assets/ADR-001-*.md` | TODO |
| IT-03 | 9 铁律机器规则正反例校验 | F1-T03 / F4-T02 | `it/test_sprint31_caliber_rules.sh` + `it/evidence/20260605-local/caliber-static-sql-regression-test.log` | PASS（2026-06-05 local static SQL regression） |
| IT-04 | pack guardrails sync 生成 diff | F2-T01/T02 | `it/test_sprint31_pack_governance_guardrails.sh` + `it/evidence/20260605-local/pack-governance-guardrails-test.log` | PASS（2026-06-05 local contract + pack gate） |
| IT-05 | 漂移检测触发 + 不可达降级演练 | F2-T03 | `it/test_sprint31_caliber_sync_drift_fallback.sh` + `it/evidence/20260605-local/caliber-sync-drift-fallback-test.log` | PASS（2026-06-05 local provider + health gate） |
| IT-06 | 跨源一致性回归（绿） | F4-T01 | `it/test_sprint31_cross_source_caliber_regression.sh` + `it/evidence/20260605-local/cross-source-caliber-regression-test.log` | PASS（2026-06-05 local fixture） |
| IT-07a | 语义草稿端点本地暂存 contract | F3-T01 | `it/test_sprint31_semantic_drafts.sh` | PASS（2026-06-05 local） |
| IT-07b | 语义草稿提交治理 DRAFT contract + live 写入 | F3-T02 | `it/test_sprint31_semantic_draft_governance_submission.sh` + `it/evidence/20260605-local/semantic-draft-live-platform-submission.md` | PASS（2026-06-05 local + live platform） |
| IT-07 | 草稿晋升闭环一例（草稿→审→正式→回流） | F3-T03 | `it/evidence/20260605-local/semantic-draft-backflow-sync-complete.md` | PASS（2026-06-05 local + live platform） |

## 目录约定

```
it/
  README.md           # 本文件
  evidence/{YYYYMMDD-env}/   # 运行日志、截图、diff
  test_*.sh           # 可重跑脚本
```
