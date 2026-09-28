# 项目域口径决策表（Sprint-25 P0）

> 本表必须由业务方拍板。状态保持 `PENDING` 时，不允许进入 dbt P1 生产建模。

## 决策 1：p_project_green 快照粒度

**问题**：业务方问“某时点/某月在摆绿植”时，DWS 应保存每日快照、月末快照，还是直接取当前实摆？

| 选项 | 处理 | 影响 |
|---|---|---|
| A | 每日快照 | 支持任意时点回溯，存储和调度成本最高 |
| B | 每月末快照 | 月报稳定，不能回答日内历史 |
| C | 当前实摆表直接汇总 | 实现快，但不能回溯历史 |

**推荐**：先 B，若业务方有“历史某天”问句再扩展 A。

**2026-05-30 数据画像**：当前 ODS 只有当前/历史摆放记录的 `pose_time`，范围 `2021-04-01` 至 `2024-09-01`；dbt baseline 已按 `pose_time` 产出 `xycyl_dws_project_green_monthly`，但未生成日快照。

**业务方填**：

- 状态：PENDING
- 拍板：
- 拍板人：
- 拍板时间：
- 例外：

## 决策 2：rent / cost 聚合口径

**问题**：`p_project_green.rent` / `cost` 是单株单价、组合单价，还是该行总额？聚合是否乘 `total_number` 或 `good_number`？

| 选项 | 处理 | 风险 |
|---|---|---|
| A | `SUM(rent)` / `SUM(cost)` | 若字段是单价会低估 |
| B | `SUM(rent * total_number)` / `SUM(cost * total_number)` | 若字段已是总额会高估 |
| C | 按 `parent_id` / `green_type` 分层处理 | 最复杂，但可能最贴近组合绿植 |

**验证方式**：与 adminweb 项目实摆总览、`ProjectSummaryMapper` / `FlowerSumMapper` 中 `SUM(IFNULL(rent,0)*IFNULL(total_number,0))` 一致性对账。

**2026-05-30 数据画像**：`p_project_green` 共 36295 行，`rent` 空值 4268 行、`cost` 无空值；raw 合计 `rent=623546.44`、`cost=1879939.26`，`total_number_sum=203845`，`good_number_sum=100138`。

**2026-05-31 adminweb 对账结论**：dbt baseline 同时保留 raw 合计和 adminweb 当前运营字段。`xycyl_ads_project_overview.rent_amount_adminweb_sum=1067755.65`、`cost_amount_adminweb_sum=4038819.03` 与 `ProjectSummaryMapper.listPage` 完全一致，live 对账 7/7 PASS。该结果只证明当前运营报表可复现，不把乘法口径自动升级为业务方最终口径。

**业务方填**：

- 状态：PENDING
- 拍板：
- 拍板人：
- 拍板时间：

## 决策 3：停用项目是否计入实摆汇总

**问题**：`p_project.status=2` 的停用项目，在“全公司实摆总览”“项目状态分布”“合同到期预警”中是否统计？

| 选项 | 处理 |
|---|---|
| A | 默认排除停用项目，仅在明细中可筛选 |
| B | 默认纳入，并用状态字段区分 |
| C | ADS 同时产出 `active_only` 与 `all` 双口径 |

**推荐**：C，沿用报花 `_finished` / `_all` 双口径思想。

**2026-05-30 数据画像**：项目状态 ADS 显示正常项目 149、停用项目 73；`xycyl_ads_project_overview` 当前为 active-only 口径，DWD 仍保留孤儿/停用项目质量标记用于追溯。

**业务方填**：

- 状态：PENDING
- 拍板：
- 拍板人：
- 拍板时间：

## 决策 4：实摆“组数”定义

**问题**：项目实摆的“组数”是 `COUNT(p_project_green.id)`，还是排除 `parent_id=-1` 顶层组合行，或按 `total_number` 求和？

| 选项 | 处理 | 适用问句 |
|---|---|---|
| A | `COUNT(id)` | 明细行数 |
| B | `COUNT(*) WHERE parent_id <> -1` | 排除顶层组合占位 |
| C | `SUM(total_number)` | 实际数量 |

**2026-05-30 数据画像**：`p_project_green` 中 `parent_id=-1` 顶层行 16086 行，子项 20209 行；`total_number_sum=203845`、`good_number_sum=100138`，三种组数/数量口径差异很大，不能由开发侧默认拍板。

**业务方填**：

- 状态：PENDING
- 拍板：
- 拍板人：
- 拍板时间：

## 决策 5：项目域默认业务时间

**问题**：项目域月报默认用 `pose_time`、`apply_time`，还是 `_dts_import_time`？

| 场景 | 推荐字段 | 说明 |
|---|---|---|
| 当前实摆月报 | `pose_time` | 实际摆放时间 |
| 摆位调整月报 | `p_position_adjustment.apply_time` | 调整申请时间 |
| 入湖健康检查 | `_dts_import_time` | 仅用于数据时效，不作业务时间 |

**2026-05-30 数据画像**：当前实摆月报按 `pose_time` 落入 239 个项目月份；摆位调整按 `apply_time` 可用于月度调整统计；`_dts_import_time` 只用于 ingestion execution `83` 的技术入湖证明。

**业务方填**：

- 状态：PENDING
- 拍板：
- 拍板人：
- 拍板时间：

## 阻塞条件

- 任一决策状态不是 `RESOLVED`：F1 dbt 生产模型不得进入实施。
- adminweb 固定报表对账误差超过 0.5%：ADS 不得晋升为项目域默认数据面。2026-05-31 已通过 ProjectSummary 对账，当前剩余阻塞仅为业务方未将 5 个决策置为 `RESOLVED`。
