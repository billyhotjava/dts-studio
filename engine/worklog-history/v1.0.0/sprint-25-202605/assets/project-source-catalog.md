# 项目域 Source Catalog（Sprint-25 P0）

> 当前基于 `adminapi/rs-api/rs-flowers-base-api` 领域对象和 `rs-flowers-base` Mapper 静态盘点，生产字段仍需用 `INFORMATION_SCHEMA.COLUMNS` 校准。稳定落库约定：dbt source 指向 `schema: public`，物理表名为 `public.ods_ptr_mysql_<业务表名>`。

## 入湖约定

| 业务表 | ODS 物理表 | 角色 | P0 状态 |
|---|---|---|---|
| `p_customer` | `public.ods_ptr_mysql_p_customer` | 客户主数据 | FOUND (2026-05-29 local biadmin), 178 rows, has `_dts_*` |
| `p_project` | `public.ods_ptr_mysql_p_project` | 项目点主数据 | FOUND (2026-05-29 local biadmin), 240 rows, has `_dts_*` |
| `p_contract` | `public.ods_ptr_mysql_p_contract` | 合同主数据 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `p_position` | `public.ods_ptr_mysql_p_position` | 摆位主数据 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `p_floor_layer` | `public.ods_ptr_mysql_p_floor_layer` | 楼层维度 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `p_floor_number` | `public.ods_ptr_mysql_p_floor_number` | 楼号维度 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `b_goods` | `public.ods_ptr_mysql_b_goods` | 物品主数据 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `b_goods_price` | `public.ods_ptr_mysql_b_goods_price` | 物品价格维度 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `p_project_green` | `public.ods_ptr_mysql_p_project_green` | 项目实摆事实 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `p_position_adjustment` | `public.ods_ptr_mysql_p_position_adjustment` | 摆位调整主表 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |
| `p_position_adjustment_item` | `public.ods_ptr_mysql_p_position_adjustment_item` | 摆位调整明细 | CREATED EMPTY (2026-05-29 local biadmin), has `_dts_*` |

## 核心字段口径

### `p_project`

- 领域对象：`com.rs.flowers.base.project.domain.Project`
- 关键字段：`id`、`contract_id`、`name`、`code`、`status`、`type`、`address`、`manager_id`、`biz_user_id`、`settle_start_time`、`settle_end_time`、`deliver_time_str`、`del_flag`
- 状态：`status=1` 正常，`status=2` 停用。停用项目是否计入实摆汇总必须由 P0 决策。
- 软删：`del_flag` 带 `@TableLogic`，stg 层统一过滤有效行。

### `p_customer`

- 领域对象：`com.rs.flowers.base.project.domain.Customer`
- 关键字段：`id`、`code`、`name`、`abbreviation`、`contacts_name`、`contacts_phone`、`contacts_post`、`type`、`status`、`source`、`address`、`del_flag`
- 用途：`xycyl_dim_customer`，后续采购和财务共用。

### `p_contract`

- 领域对象：`com.rs.flowers.base.project.domain.Contract`
- 关键字段：`id`、`customer_id`、`code`、`title`、`signing_time`、`status`、`start_date`、`end_date`、`settlement_method`、`settlement_type`、`verify_type`、`verify_day_number`、`month_settlement_money`、`discount_ratio`、`parent_id`、`active_child`、`del_flag`
- 状态：`1` 草稿，`2` 履行中，`3` 已结束。
- 用途：`xycyl_dim_contract` 以及打宽 `xycyl_dim_project`。

### `p_position`

- 领域对象：`com.rs.flowers.base.project.domain.Position`
- 关键字段：`id`、`project_id`、`type`、`floor_number_id`、`floor_number_name`、`floor_layer_id`、`floor_layer_name`、`region`、`region_full`、`curing_period`、`follow_type`、`curing_user_id`、`curing_user_name`、`alias_name`、`status`、`del_flag`
- 状态：`status=0` 正常，`status=1` 停用。
- 用途：`xycyl_dim_position`，需 JOIN `p_floor_layer` / `p_floor_number` 补齐层级名。

### `b_goods` / `b_goods_price`

- 领域对象：`Goods` / `GoodsPrice`
- `b_goods` 关键字段：`id`、`name`、`code`、`type`、`unit`、`specifications`、`status`、`category`、`goods_classify_id`、`del_flag`
- `b_goods_price` 关键字段：`id`、`goods_id`、`goods_code`、`goods_name`、`specifications`、`goods_att`、`unit`、`goods_type`、`guidance_price`、`cost_price`、`good_category`、`status`
- 用途：`xycyl_dim_goods`。采购域和项目实摆都要按 `good_price_id` 关联。

### `p_project_green`

- 领域对象：`com.rs.flowers.base.project.domain.ProjectGreen`
- 关键字段：`id`、`project_id`、`project_name`、`position_id`、`position_full_name`、`green_type`、`good_name`、`good_price_id`、`good_type`、`good_norms`、`good_specs`、`status`、`rent_mode`、`rent`、`cost`、`pose_time`、`parent_id`、`good_number`、`total_number`、`import_status`、`lock_change_number`、`lock_cut_number`、`lock_transfer_number`、`lock_bad_number`、`lock_adjustment_number`
- 状态：`1` 摆放中，`2` 换花中，`3` 加花中，`4` 减花中，`5` 调花中，`6` 坏账处理中，`7` 已结束。
- 已知陷阱：`parent_id=-1` 顶层行和子项行会影响“组数”定义；`rent` / `cost` 可能是单价，聚合需结合 `total_number` 和 `good_number` 决策。

### `p_position_adjustment` / `p_position_adjustment_item`

- 领域对象：`PositionAdjustment` / `PositionAdjustmentItem`
- 主表关键字段：`id`、`code`、`title`、`project_id`、`project_name`、`apply_user_id`、`apply_user_name`、`apply_time`、`status`、`total_adjustment_number`、`curing_user_id`、`curing_user_name`、`adjustment_type`
- 明细关键字段：`id`、`position_adjustment_id`、`old_position_id`、`new_position_id`、`good_price_id`、`good_name`、`adjustment_number`、`old_green_id`、`rent`、`cost`、`parent_id`
- 状态：主表 `-1` 已作废，`0` 待确认，`1` 已结束，`10` 草稿。

## 数据画像 SQL 清单

每张表至少跑：

```sql
SELECT COUNT(*) AS total_rows FROM public.ods_ptr_mysql_p_project;
SELECT status, COUNT(*) AS cnt FROM public.ods_ptr_mysql_p_project GROUP BY status ORDER BY cnt DESC;
SELECT status, import_status, parent_id, COUNT(*) AS cnt
FROM public.ods_ptr_mysql_p_project_green
GROUP BY status, import_status, parent_id
ORDER BY cnt DESC;
```

软外键孤儿率：

```sql
SELECT COUNT(*) AS orphan_project_green
FROM public.ods_ptr_mysql_p_project_green g
LEFT JOIN public.ods_ptr_mysql_p_project p ON p.id = g.project_id
WHERE p.id IS NULL;

SELECT COUNT(*) AS orphan_position_adjustment_item
FROM public.ods_ptr_mysql_p_position_adjustment_item i
LEFT JOIN public.ods_ptr_mysql_p_position_adjustment a ON a.id = i.position_adjustment_id
WHERE a.id IS NULL;
```

## P0 必查问题

- `public.ods_ptr_mysql_p_project_green` 等 9 张缺失 ODS 已在本地 DTS 补齐为空表（2026-05-29 local biadmin），但尚未导入源业务数据；F0-T02 仍不能给出事实画像结论。
- `p_project_green.status` 7 态是否在生产实际出现，是否存在文档外值。
- `import_status=2` 是否能作为“已确认实摆”的稳定过滤条件。
- `parent_id=-1` 与子项行的占比，决定实摆组数是否要排除顶层占位。
- `rent` / `cost` 与 `total_number` 的乘法口径，要和 adminweb 固定报表 SQL 对账。
