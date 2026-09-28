# T02: 远程数据 IT 脚本

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

参考 sprint-20 `test_procurement_query_regression.sh` 形态，编写 `test_flowerbiz_query_regression.sh`，让报花域 12 条回归记录在远程库可一键执行。

## 脚本骨架

```bash
#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${FLOWER_DB_HOST:-db.weitaor.com}"
DB_PORT="${FLOWER_DB_PORT:-3306}"
DB_USER="${FLOWER_DB_USER:-flowerai}"
DB_NAME="${FLOWER_DB_NAME:-rs_cloud_flower}"
DB_PASSWORD="${FLOWER_DB_PASSWORD:-}"

[[ -z "${DB_PASSWORD}" ]] && { echo "FLOWER_DB_PASSWORD is required" >&2; exit 1; }

run_sql() {
  MYSQL_PWD="${DB_PASSWORD}" mysql \
    -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -D "${DB_NAME}" \
    -N -B -e "$1"
}

# === R1: 本月加摆汇总（与 ads_lease_summary 对照）===
echo "== R1: lease_in_amount this month =="
r1_total=$(run_sql "SELECT ROUND(SUM(i.rent * i.plant_number),2) FROM t_flower_biz_info f JOIN t_flower_biz_item i ON i.flower_biz_id=f.id WHERE f.biz_type=2 AND f.status=5 AND f.del_flag='0' AND i.del_flag='0' AND DATE_FORMAT(i.start_time,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m');")
echo "lease_in_amount=${r1_total}"

# === R2: 审核中超过 7 天 ===
echo "== R2: pending overdue 7d =="
r2=$(run_sql "SELECT COUNT(*) FROM t_flower_biz_info WHERE status=1 AND del_flag='0' AND DATEDIFF(CURDATE(), apply_time) > 7;")
echo "overdue_count=${r2}"

# === R3 ~ R12: ===（按 R1 模式扩展）

echo "Flowerbiz regression baseline passed."
```

## 数据样本采集

实施时先在远程库跑 baseline SQL：

```sql
-- 当前月加摆 in
SELECT ROUND(SUM(i.rent*i.plant_number),2) FROM t_flower_biz_info f JOIN t_flower_biz_item i ON i.flower_biz_id=f.id WHERE f.biz_type=2 AND f.status=5 AND f.del_flag='0' AND i.del_flag='0' AND DATE_FORMAT(i.start_time,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m');

-- 13 bizType 实际分布
SELECT biz_type, COUNT(*) FROM t_flower_biz_info WHERE del_flag='0' GROUP BY biz_type ORDER BY 2 DESC;

-- 7 状态实际分布
SELECT status, COUNT(*) FROM t_flower_biz_info WHERE del_flag='0' GROUP BY status ORDER BY 2 DESC;

-- 当月加摆 TOP 项目
SELECT p.name, ROUND(SUM(i.rent*i.plant_number),2) FROM t_flower_biz_info f JOIN t_flower_biz_item i ON i.flower_biz_id=f.id JOIN p_project p ON p.id=f.project_id WHERE f.biz_type=2 AND f.status=5 AND f.del_flag='0' AND DATE_FORMAT(i.start_time,'%Y-%m')=DATE_FORMAT(CURDATE(),'%Y-%m') GROUP BY p.name ORDER BY 2 DESC LIMIT 10;
```

把结果写进 `flowerbiz-query-regression.md` 数据样本。

## 数据漂移处理

报花数据日变，硬断言不能写死金额。策略：

- Top1 项目加摆金额：只断言 ≥ Top2（排序正确）
- 月份记录数：只断言 > 0
- 当月销售总额：只断言"> 0 且与上次跑差 < 10%"

## 影响范围

- `it/test_flowerbiz_query_regression.sh` —— 新增（chmod +x）
- `it/flowerbiz-query-regression.md` —— T01 输出，本 task 补数据样本

## 验证

- [ ] 脚本远程库通过
- [ ] 至少 5 条断言（R1/R2/R5/R7/R10）有具体数值或范围检查
- [ ] 执行时间 < 30 秒

## 完成标准

- [ ] IT 脚本可执行
- [ ] 12 条回归条目至少 10 条在脚本中有覆盖
- [ ] CI 集成或手动执行步骤明确
