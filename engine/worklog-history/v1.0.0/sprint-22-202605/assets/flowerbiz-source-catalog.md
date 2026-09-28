# 报花域 Source / Mart 完整字段口径文档（基线）

> 本文件是 sprint-22 的单一事实来源（SOT），由 F1-T01 在实施时根据生产库实际情况校对修正。
>
> **校对前**：以 adminapi 代码 review（2026-05）和 sprint-21 落地视图为基础。
> **校对后**：所有字段必须与 `INFORMATION_SCHEMA.COLUMNS` 输出 1:1 对齐。

## 一、报花生命周期总览

```
[创建(20 草稿)]
   ↓ 提交
[审核中(1)] ──→ [驳回(21)] ─→ 草稿 ↻
   ↓ 审核通过
[备货中(2)] ─→ 触发采购(异步) ─→ 配送 ─→ 入库
   ↓ 备货完成
[核算中(3)] ─→ [变更(ChangeInfo)] 重新核算
   ↓ 核算完成
[待结算(4)] ─→ 加摆/撤摆/变更后续操作 ⤴
   ↓ AutoReceiptService 自动结算
[已结束(5)] ─→ 触发回收 ─→ 入库

[作废(-1)] ← 任意状态强制取消
```

## 二、13 bizType 完整语义

| bizType | 中文术语 | 触发场景 | 金额方向 | 关联模块联动 | adminweb 入口 |
|---|---|---|---|---|---|
| 1 | 换花 | 同摆位替换品种 | 中性（rent 不变） | 无下游联动 | `flowerbiz/biz/list-biz.vue` |
| 2 | 加花/加摆 | 客户增订 / 新摆位 | **+正（rent 增）** | 触发 PlanPurchaseItem | `flowerbiz/add/list-add-flower.vue` |
| 3 | 减花/撤摆 | 客户退订 / 减摆位 | **-负（rent 减）** | 触发 RecoveryInfo | `flowerbiz/cut/list-cut-flower.vue` |
| 4 | 调花/调拨 | 跨摆位 / 跨库房 | 中性（内部对冲） | 跨摆位锁 lock_transfer_number | `flowerbiz/transfer/list-transfer-flower.vue` |
| 6 | 坏账 | 客户欠款核销 | **-负** | 走 ISaleAccountService | `flowerbiz/baddebt/list-baddebt-flower.vue` |
| 7 | 售花/销售 | 有偿出售 | +正 | **走 ISaleAccountService** | `flower/sale/` |
| 8 | 赠花 | 无偿赠送 | 0 | **走 ISaleAccountService** | `flower/sale/` (赠送子模块) |
| 10 | 配料 | 添加辅料（花盆/肥料）| +正 | 标准 | `flowerbiz/add/` (含辅料) |
| 11 | 加盆架 | 增加盆架 | +正 | 标准 | `flowerbiz/addBasket/list-add-basket.vue` |
| 12 | 减盆架 | 减少盆架 | -负 | 标准 | (在 cut 内) |

> bizType=5 / 9 / 13 等不在文档值列表中，需 F0-T04 数据画像 `SELECT DISTINCT biz_type` 校对实际是否存在。

## 三、7 张报花源表

### 3.1 t_flower_biz_info（报花单主表）

**业务角色**：业务事实表的源头。每行 = 一张报花单 = 一次 bizType 实例。

| 字段 | 类型 | 业务含义 | 备注 |
|---|---|---|---|
| `id` | BIGINT PK | | |
| `code` | VARCHAR | 单据编号 | 业务用，唯一 |
| `biz_type` | INT | 业务类型枚举 | 见上面 13 bizType |
| `status` | INT | 报花单状态 | -1/1/2/3/4/5/20/21 |
| `project_id` | BIGINT | p_project.id | 软外键 |
| `apply_user_id` | BIGINT | 申请人（养护/项目/业务）| u_personnel.user_id |
| `apply_time` | DATETIME | 申请时间 | 与 create_time 可能不一致 |
| `plan_finish_time` | DATETIME | 计划完工时间 | |
| `settlement_time` | DATETIME | 结算完成时间 | NULL 表示未结算 |
| `biz_total_rent` | DECIMAL(14,2) | 报花单总租金 | **金额方向因 bizType 异**（见 dim_biztype）|
| `biz_total_cost` | DECIMAL(14,2) | 成本 | |
| `total_amount` | DECIMAL(14,2) | 总金额 | 销售场景用 |
| `examine_user_id` | BIGINT | 审核人 | NULL 表未审 |
| `sign_user_id` | BIGINT | 签字人 | |
| `curing_user_id` | BIGINT | 养护人 | |
| `lease_term` | INT | 是否填写租期 | |
| `start_lease_time` | DATETIME | 起租时间 | **可被 t_flower_rent_time_log 事后修改** |
| `del_flag` | CHAR(1) | 软删 | '0' 有效 |
| `create_time` | DATETIME | 创建时间 | 仅审计 |
| `update_time` | DATETIME | 更新时间 | 多 service 写，状态竞态可能性 |

**陷阱**：
- `status` 在 4 个 service 同时写（FlowerBizInfoService / FlowerBizFinishService / DistributionSrevice / AutoReceiptService）
- `start_lease_time` 可事后修改

### 3.2 t_flower_biz_item（报花明细）

**粒度**：每行 = 一个摆位上一棵绿植。多对一至 t_flower_biz_info（**软外键**）。

| 字段 | 类型 | 业务含义 |
|---|---|---|
| `id` | BIGINT PK | |
| `flower_biz_id` | BIGINT | t_flower_biz_info.id（软外键） |
| `position_id` | BIGINT | p_position.id 摆位 |
| `biz_type` | INT | 本明细的业务类型（与主表 biz_type 一致）|
| `status` | INT | 明细状态（在多 service 写） |
| `good_price_id` | BIGINT | 商品价格 ID（决定 rent / cost） |
| `good_name` | VARCHAR | 绿植品种名 |
| `plant_number` | INT | 数量（加+ 减-）|
| `rent` | DECIMAL(14,2) | 单棵月租金 |
| `cost` | DECIMAL(14,2) | 单棵成本 |
| `put_time` | DATETIME | 摆放时间 |
| `start_time` | DATETIME | 起租时间（可事后改）|
| `end_time` | DATETIME | 停租时间 |
| `net_receipts_number` | INT | 回收时净收回数量 |
| `frm_loss_number` | INT | 回收时报损数量 |
| `buyback_number` | INT | 回收时回购数量 |
| `keep_number` | INT | 回收时留用数量 |
| `del_flag`, `create_time`, `update_time` | | |

### 3.3 t_flower_biz_item_detailed

子项明细，关联实摆 p_project_green_item。粒度更细，本 sprint 暂不建对应 mart。

### 3.4 t_change_info

**变更单**。报花单 status≥4 后的金额/起租期/规格变更。

| 字段 | 业务含义 |
|---|---|
| `id` PK | |
| `code` | 变更单号 |
| `biz_id` | t_flower_biz_info.id |
| `change_type` | 1 销售金额变更 / 2 库房物品类型 / 3 成本 / 4 起租减租 |
| `before_total_amount` / `after_total_amount` | 金额变更前后 |
| `before_settlement_time` / `after_settlement_time` | 起租期变更前后 |
| `status` | 1 确认中 / 2 已结束 / -1 作废 |

### 3.5 t_recovery_info（回收主表）

| 字段 | 业务含义 |
|---|---|
| `id` PK | |
| `biz_info_id` | t_flower_biz_info.id |
| `distribution_user_id` | 配送人 |
| `recovery_user_id` | 回收人 |
| `store_house_id` | 入库目标库房 |
| `plan_recovery_time` | 计划回收时间 |
| `recovery_time` | 实际回收时间 |
| `status` | 1 待回收 / 2 确认入库 / 3 已结束 |

### 3.6 t_recovery_info_item

| 字段 | 业务含义 |
|---|---|
| `recovery_info_id` | 主表 |
| `biz_item_id` | t_flower_biz_item.id |
| `recovery_type` | 1 报损 / 2 回购 / 3 留用 |
| `recovery_number` | 计划回收数 |
| `real_recovery_number` | **实际回收数（业务关心）** |
| `good_cost` | 单棵成本 |

### 3.7 t_flower_rent_time_log

| 字段 | 业务含义 |
|---|---|
| `biz_id` | t_flower_biz_info.id |
| `rent_time_type` | 1 起租 / 2 减租 |
| `old_rent_time` | 旧时间 |
| `new_rent_time` | 新时间 |

### 3.8 t_flower_biz_log（报花单操作日志，2026-05 补盘点）

**业务角色**：报花单的状态机流水。每次提交/审核/驳回/备货/入库/结算/作废都有一条记录。

| 字段 | 业务含义 |
|---|---|
| `id` PK | |
| `biz_id` | t_flower_biz_info.id（软外键）|
| `biz_type` | 与主表 biz_type 一致 |
| `status` | 操作时该单据的状态 |
| `sorts` | 顺序号 |
| `operation_title` | 操作动作名称（自由文本，如"项目经理审核通过"）|
| `operation_user_id` / `operation_user_name` | 操作人 |
| `operation_time` | 操作时间 |
| `operation_content` | 操作内容/原因 |

**用途**：审批耗时、操作链路、谁经手、X 单挂在哪一步。**本表无 `del_flag`**，全量取数。

### 3.9 t_flower_extra_cost（报花额外费用，2026-05 补盘点）

**业务角色**：报花单的运费/人工/税费/垃圾清理 等附加成本，影响真实成本核算。一张报花单可有多条。

| 字段 | 业务含义 |
|---|---|
| `id` PK | |
| `biz_id` | t_flower_biz_info.id（软外键）|
| `biz_type` | 报花业务类型 |
| `cost_type` | 1 运费 / 2 人工 / 3 税费 / 4 其他 / 5 垃圾清理 |
| `title` | 费用名称 |
| `free_amount` | 不含税成本金额 |
| `price_amount` | **含税金额（业务方报销看这个）** |
| `tax_rate` | 税率 |
| `pay_user_id` / `pay_user_name` / `pay_time` | 支付人/时间 |
| `expense_id` / `expense_code` | 关联报销单 |

**用途**：项目附加成本占比、TOP 项目、成本类型构成。

## 四、报花联动的下游表（不在本 sprint mart 范围）

| 下游表 | 与报花的关系 | sprint |
|---|---|---|
| `t_plan_purchase_info` / `t_plan_purchase_item` | bizType=2 加花异步触发 | sprint-23 采购域 |
| `t_delivery_info` / `t_warehousing_info` | 备货中 → 核算中 | sprint-23 |
| `p_project_green` / `p_project_green_item` | 已结束写入实摆清单 | sprint-24 摆放域 |
| `t_settlement_info` / `t_settlement_item` | 待结算时聚合（biz_type≠7,8）| sprint-25 财务域 |
| `a_month_accounting` | 月结聚合 | sprint-25 |
| `a_sale_account` | bizType=6/7/8 走这里（实际表名带 `a_` 前缀） | sprint-25 |
| `a_flower_biz_accounting` | 结算加花明细，**bizType 体系不同**（4=调减/5=调加 vs 主表 4=调花统称）| sprint-25 |

## 五、问句决策表（关键！）

| 问句关键词 | 主视图（mart）|
|---|---|
| 加摆 / 撤摆 / 净增 / 净减 / 月度报花 | `xycyl_ads_flowerbiz_lease_summary` |
| XX 项目 / XX 客户 + 最近报花 / 报花单 | `xycyl_ads_flowerbiz_lease_detail` |
| 审核中超过 / 备货中超过 / 待结算超过 / 挂起 / 滞留 | `xycyl_ads_flowerbiz_pending` |
| 售花 / 销售 / 赠花 / 卖花 | `xycyl_ads_flowerbiz_sale_summary` |
| 坏账 / 死账 / 客户欠款（短期，sprint-25 后走 finance）| `xycyl_ads_flowerbiz_baddebt_summary` |
| 起租期变更 / 金额变更 / 规格变更 | `xycyl_ads_flowerbiz_change_log` |
| 回收 / 报损 / 回购 / 留用 | `xycyl_ads_flowerbiz_recovery_detail` |
| 养护人 / 师傅工作量 / 经手 | `xycyl_ads_flowerbiz_curing_workload` |
| 审批耗时 / 操作链路 / X 单挂在哪 / 谁审的 | `xycyl_ads_flowerbiz_audit_trail` |
| 运费 / 人工费 / 税费 / 垃圾清理 / 附加成本 | `xycyl_ads_flowerbiz_extra_cost_summary` |

## 六、字段口径一览

| 类别 | 规则 |
|---|---|
| 业务时间 | `start_lease_time`（已被 dws/ads 层归口为"业务月份"），create_time 仅审计 |
| 金额精度 | 所有 `*_amount` `DECIMAL(14,2)`，下游 SUM 必须 `ROUND(...,2)` |
| 金额方向 | dim_biztype_alias 带 `amount_direction`（in/out/neutral），ads 层按方向 SUM |
| 状态翻译 | dim_status_alias / dim_biztype_alias / dim_recovery_type_alias / dim_change_type_alias 四张维度表 |
| 软删 | 所有 stg 已过滤 `del_flag='0'`，下游不重复加 |
| 业务结束 | `status = 5` 表示"已结束"，是 ads 默认过滤；进行中走 pending mart |

## 七、6 个真实陷阱与应对

| 陷阱 | 应对 |
|---|---|
| 异步触发（@Async）：PlanPurchaseItem 不立刻有 | mart 物化前提是上下游已对齐；ads 加 `data_freshness_minutes` 元数据 |
| 软外键：item.flower_biz_id 无 DB 约束 | dwd 全部 LEFT JOIN；schema.yml relationships 测试 severity=warn |
| 状态竞态：item.status 多 service 写 | incremental 用 `update_time` 不用 `status` 作 unique_key |
| 金额符号：bizType 决定方向 | dim 带 amount_direction；dwd ABS + 方向；ads 按方向 SUM |
| 链路分叉：bizType=7/8 走 SaleAccount | 拆 lease / sale / baddebt 三个独立 ads；UNION 视图作可选总览 |
| 时间不可靠：start_time 可事后改 | 业务时间字段从 F0-T03 决策（默认 start_lease_time，已通过 t_flower_rent_time_log 跟踪） |

## 八、反例（禁止重复出现）

| 错误 | 正确 |
|---|---|
| `SUM(biz_total_rent)` 不分组算"本月报花总额" | 按 biz_type 拆，或走 lease_summary + sale_summary UNION |
| `status=1` 数字硬编码判审核中 | 用 dim_status_alias 翻译为中文，或用 `xycyl_ads_flowerbiz_pending` 的 `状态='审核中'` |
| 用 create_time 算业务时间 | 用 start_lease_time |
| INNER JOIN 报花 item ↔ info | LEFT JOIN（软外键有孤儿） |
| 销售 SUM 进 lease_summary | 必须走 sale_summary |
| 看 recovery_number 而非 real_recovery_number | 业务关心实际值 |
| 算调花金额（bizType=4 是中性） | 调花看数量，不看金额 |
