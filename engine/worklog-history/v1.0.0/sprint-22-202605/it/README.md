# Sprint-22 IT

本目录用于存放双轨产出物的回归脚本、验收矩阵、真人联调清单与运行记录：

- **轨 1**（dts-copilot 智能层）：finance.json + 路由 + 模板 / few-shots 的语义化回归
- **轨 2**（dts-stack 治理层）：dbt 产出 vs sprint-21 PG 视图一致性回归

## 自动化回归

### 轨 1：财务问句回归（dts-copilot 路径）

- 脚本：`it/test_finance_query_regression.sh`
- 覆盖：12 条财务典型问句的远程数据回归
- 运行方式：

```bash
FLOWER_DB_PASSWORD='***' \
bash worklog/v1.0.0/sprint-22-202605/it/test_finance_query_regression.sh
```

- 脚本入口要求：远程库 `rs_cloud_flower` 上 `authority.finance.*` 与 `mart.finance.customer_ar_rank_daily` 已落地
- 失败时输出 `Rxx baseline mismatch` 并退出码非 0

### 轨 2：dbt 产出 vs PG 视图一致性回归（F4 产出）

- 脚本：`it/test_xycyl_dbt_consistency.sh`
- 覆盖：5+ 条代表性 SQL 在两个 datasource（旧 PG 视图 / 新 dbt 产出）跑出结果做 diff
- 期望：行数 100% 一致，金额差 < 0.01 元
- 运行方式：

```bash
COPILOT_API_KEY='cpk_xxx' \
OLD_DS_ID=1 NEW_DS_ID=99 \
bash worklog/v1.0.0/sprint-22-202605/it/test_xycyl_dbt_consistency.sh
```

- 脚本入口要求：F4-T02/T03/T05 已完成，dbt 产出库可达，dts-copilot datasource ID 99 已注册
- 失败时输出 `MISMATCH for query: ...` 并退出码非 0

## 验收矩阵

- 文档：`it/acceptance-matrix.md`
- 用途：对齐财务域固定报表 + NL2SQL 路由 + 字段语义 + 字典翻译 + 跨域路由 + Guardrails 六维验收

## 回归基线

- 文档：`it/finance-query-regression.md`
- 用途：12 条财务典型问句的"问句 → 期望路由 → 期望视图 → SQL 关键特征 → 期望结果"基线

## 真人联调

- 清单：`features/F3-财务域回归与验收/T03-验收矩阵更新与真人联调.md`
- 运行记录：`it/finance-acceptance-runs.md`
- 至少要求一次完整 A/B/C/D 四类联调通过

## 输出要求

每次真人联调结束后，至少记录：

- 日期
- 环境（远程域名）
- 使用账号
- A/B/C/D 通过情况
- 失败时对应路径、截图、日志
- 结论：是否纳入 sprint-22 DONE
