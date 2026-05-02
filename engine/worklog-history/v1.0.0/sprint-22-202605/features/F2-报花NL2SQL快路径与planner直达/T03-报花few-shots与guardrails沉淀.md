# T03: 报花 few-shots 与 guardrails 沉淀

**优先级**: P0
**状态**: READY
**依赖**: F1-T02, F2-T01, F2-T02

## 目标

把 flowerbiz.json 内置的 fewShots / guardrails 与 F0-T01 真实问句、6 个真实陷阱打通，让 AGENT_WORKFLOW 路径下 LLM prompt 含足够强的报花域上下文。

## 输出物

### prompts/flowerbiz-few-shots.txt（仅 AGENT_WORKFLOW 路径注入）

参考 `prompts/settlement-few-shots.txt` 格式，10 条长例子覆盖：

- 加摆撤摆汇总（`FB-Q-LEASE-NET-MONTHLY` 类）
- 项目级最近报花（`FB-Q-LEASE-DETAIL-RECENT` 类）
- 待处理超期（`FB-Q-PENDING-OVERDUE` 类）
- 销售排行（`FB-Q-SALE-MONTHLY-RANK` 类）
- 坏账客户（`FB-Q-BADDEBT-BY-CUSTOMER` 类）
- 起租期变更（`FB-Q-CHANGE-LOG-RECENT` 类）
- 回收明细（`FB-Q-RECOVERY-BY-PROJECT` 类）
- 养护人工作量（`FB-Q-CURING-WORKLOAD` 类）
- 新签客户首次报花（`FB-Q-FIRST-LEASE-BY-CUSTOMER` 类）
- 累计加摆（`FB-Q-LEASE-CUMULATIVE` 类）

### prompts/flowerbiz-constraints.txt（硬约束）

```
[FLOWERBIZ DOMAIN HARD CONSTRAINTS]

1. 13 bizType 金额方向不一（加+ 撤- 调0）。SUM(金额) 必须按 bizType 或 biz_category 分组。
   优先用 ads 层已规范化的字段：加摆金额 / 撤摆金额 / 净增金额 / 销售金额 / 坏账金额。
2. 销售（biz_type=7）/赠花（biz_type=8）走另一条结算链 ISaleAccountService，不在 lease_summary。
   "本月所有报花收入"必须 UNION xycyl_ads_flowerbiz_lease_summary + xycyl_ads_flowerbiz_sale_summary。
3. 业务时间统一用 start_lease_time（已被 dws/ads 层按月归口为 业务月份）。create_time 仅用于审计。
4. 已结束（status=5）vs 进行中：lease_summary 默认仅含 status=5；"待处理"问句必须走 xycyl_ads_flowerbiz_pending。
5. 起租时间可被事后修改，通过 t_flower_rent_time_log 跟踪。
   "最近租期变更过的"走 xycyl_ads_flowerbiz_change_log，不是 lease_detail。
6. 回收数量 vs 实际回收数量：业务关心 real_recovery_number，不是 recovery_number。
7. 金额聚合必须 ROUND(...,2)。
8. project_name / customer_name / curing_user 用 LIKE '%X%'；状态、bizType、biz_category 用精确等值。
9. 软外键孤儿数据：item LEFT JOIN info 时如 info 为 NULL，业务上"忽略不计"，不要 INNER JOIN。
```

### assets/flowerbiz-guardrail-cases.md（失败回归集）

| Case | 错误问法 | LLM 容易写错的 SQL | 正确做法 |
|---|---|---|---|
| C1 | 本月报花总金额 | `SUM(biz_total_rent) FROM t_flower_biz_info` | 必须按 bizType 拆，且建议走 lease_summary + sale_summary UNION |
| C2 | 客户欠多少 | 走 `t_flower_biz_info` biz_type=6 现场聚合 | 走 baddebt_summary |
| C3 | 万象城最近的报花 | 拼 `t_flower_biz_info` + `t_flower_biz_item` | 走 `xycyl_ads_flowerbiz_lease_detail` |
| C4 | 审核中的单 | 用 `status=1` 数字硬编码 | 走 `xycyl_ads_flowerbiz_pending` + `状态='审核中'` |
| C5 | 起租期被改的单 | 拼 `t_flower_rent_time_log` JOIN | 走 `xycyl_ads_flowerbiz_change_log` |
| C6 | 回收报损 | 不区分 recovery_number vs real_recovery_number | 业务关心 real_recovery_number |
| C7 | 销售总额 | 走 settlement_summary | 走 sale_summary |
| C8 | 调拨数量 | bizType=4 算金额 | 调拨金额是 0（中性），看的是数量 |

## 影响范围

- `dts-copilot-ai/src/main/resources/prompts/flowerbiz-few-shots.txt` —— 新增
- `dts-copilot-ai/src/main/resources/prompts/flowerbiz-constraints.txt` —— 新增
- `worklog/v1.0.0/sprint-22-202605/assets/flowerbiz-guardrail-cases.md` —— 新增

## 验证

- [ ] 8 个 case 中的问句在 ChatPanel 不再命中错链
- [ ] AGENT_WORKFLOW 路径 prompt 末尾注入 flowerbiz-constraints
- [ ] 历史失败问句（如有 ai_audit_log）100% 在 guardrail-cases.md 找到对应

## 完成标准

- [ ] 10 条长 fewShots 沉淀
- [ ] 9 条硬 constraints 沉淀
- [ ] 8 条 guardrail case 文档化
