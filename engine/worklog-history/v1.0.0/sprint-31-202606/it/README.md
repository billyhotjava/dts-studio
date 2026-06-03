# Sprint-31 集成验证（IT）

**状态**: READY（待执行填充真实证据）

本目录汇总 Sprint-31 语义口径收口的可重跑验证证据。证据由各 Feature 完成时回填，**禁止空占位**（沿用 sprint-30 `it/` 标准）。

## 证据清单（待回填）

| 编号 | 验证项 | 来源 | 重跑方式 | 状态 |
|------|--------|------|----------|------|
| IT-01 | 三源口径差异矩阵 | F1-T01 | `assets/caliber-source-diff-matrix.md` | TODO |
| IT-02 | 定源 ADR 签署 | F1-T04 | `assets/ADR-001-*.md` | TODO |
| IT-03 | 9 铁律机器规则正反例校验 | F1-T03 / F4-T02 | 测试日志 | TODO |
| IT-04 | pack guardrails sync 生成 diff | F2-T02 | 生成前后对比 | TODO |
| IT-05 | 漂移检测触发 + 不可达降级演练 | F2-T03 | `it/evidence/*.log` | TODO |
| IT-06 | 跨源一致性回归（绿） | F4-T01 | 测试运行日志 | TODO |
| IT-07 | 草稿晋升闭环一例（草稿→审→正式→回流） | F3-T03 | 全链路截图/日志 | TODO |

## 目录约定

```
it/
  README.md           # 本文件
  evidence/{YYYYMMDD-env}/   # 运行日志、截图、diff
  test_*.sh           # 可重跑脚本
```
