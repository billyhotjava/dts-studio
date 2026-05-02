# F3: 报花域回归与验收

**优先级**: P1
**状态**: READY
**轨**: 1（dts-copilot 智能层）

## 目标

为报花域语义包建立可执行回归基线，覆盖 8 张 ads mart + 12+ 条典型 NL2SQL 问句，让远程 `ai.yuzhicloud.com` 上的报花问句持续可验证。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 报花域典型问句回归集 | P0 | READY | F1, F2 |
| T02 | 远程数据 IT 脚本 | P0 | READY | T01 |
| T03 | 验收矩阵更新与真人联调清单 | P1 | READY | T01, T02 |

## 完成标准

- [ ] `test_flowerbiz_query_regression.sh` 可执行且 baseline 通过
- [ ] 验收矩阵覆盖 8 张 ads mart + 12 条典型问句 + 6 个真实陷阱反例
- [ ] 真人联调清单含 5 类消费者各自典型问句
- [ ] **双轨衔接**：F4 完成后追加 `test_xycyl_dbt_consistency.sh` 验证 dbt 产出 vs adminapi 实时一致
