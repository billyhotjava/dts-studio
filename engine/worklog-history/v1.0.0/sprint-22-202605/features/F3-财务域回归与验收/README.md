# F3: 财务域回归与验收

**优先级**: P1
**状态**: READY

## 目标

为财务域语义包建立可执行回归基线，覆盖 8 个固定报表 templateCode 和 8+ 条典型 NL2SQL 问句，让远程 `ai.yuzhicloud.com` 上的财务数据面持续可验证。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 财务域典型问句回归集 | P0 | READY | F1, F2 完成 |
| T02 | 远程数据 IT 脚本 | P0 | READY | T01 |
| T03 | 验收矩阵更新与真人联调清单 | P1 | READY | T01, T02 |

## 完成标准

- [ ] 远程财务回归脚本（`test_finance_query_regression.sh`）可执行且 baseline 通过
- [ ] 验收矩阵覆盖 8 张固定报表 + 8 条典型问句
- [ ] 真人联调清单可被业务测试人员独立执行
- [ ] **双轨衔接**：F4 完成后追加 `test_xycyl_dbt_consistency.sh` 验证旧 / 新 datasource 结果一致（详见 F4-T05）
- [ ] 真人联调清单含"切换 datasource 后问句行为不变"的验证项
