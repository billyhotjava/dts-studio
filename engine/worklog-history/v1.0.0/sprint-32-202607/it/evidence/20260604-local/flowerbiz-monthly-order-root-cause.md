# 2026-03 报花单据查询失败根因与验证

## 现象

用户输入 `2026年3月的报花单据查下` 后，Agent 走业务对象画像自由 SQL，自动预览失败：

- Trino 报错：`Cannot apply operator: timestamp(0) <= varchar(19)`
- 修正时间字面量后，继续暴露字段问题：`Column 'f.curr_customer_name' cannot be resolved`

## 根因

1. 业务对象自由 SQL 使用了裸字符串时间：
   - `f.create_time >= '2026-03-01 00:00:00'`
   - Trino 联邦入口要求 timestamp 字段与 `TIMESTAMP '...'` 或 timestamp 表达式比较。
2. LLM 猜测了不存在的源表字段：
   - 错误字段：`curr_customer_name`、`proj_manager_name`、`apply_user_name`
   - 真实字段：`customer_name`、`project_manage_name`、`apply_use_name`

## 修复

- Analytics 执行层 `DefaultFederatedNativeSqlQualifier` 增加 Trino 时间字段裸字面量归一化。
- AI 模板库新增 `TPL-55`，将“某月报花单据明细”固定到真实 MySQL 字段，避免该高频问法继续走自由生成。

## 验证

- `TPL-55` 已通过 Liquibase 落库并启用：
  - `template_code=TPL-55`
  - `target_view=mysql.rs_cloud_flower.t_flower_biz_info`
- Agent 对 `2026年3月的报花单据查下` 命中：
  - `responseKind=TEMPLATE_SQL`
  - `templateCode=TPL-55`
  - `targetView=mysql.rs_cloud_flower.t_flower_biz_info`
- `/api/dataset` 自动预览成功返回表头，无 SQL 执行错误。
- 当前源库 2026-03 无报花单数据：
  - `create_time` 口径：0 行
  - `apply_time` 口径：0 行
