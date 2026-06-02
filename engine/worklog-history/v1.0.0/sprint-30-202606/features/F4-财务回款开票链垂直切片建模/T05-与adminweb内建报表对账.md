# T05: 与 adminweb 内建报表对账

**优先级**: P1
**状态**: DONE
**依赖**: T01,T02,T03

## 目标

把财务 ads 口径与 adminweb 现有内建报表对账,证明 dts-copilot 取数与业务系统一致,误差达标。

## 技术设计

- 对账标的:
  - `adminweb/src/views/flower/report/rentalMonthlyReport/index.vue`(租摆账单汇总:出账单数/已开票/已回款/账单率)
  - `adminweb/src/views/flower/report/cost/list-report.vue`(月度财务报表 r_month_reprot)
- 选若干项目×月,对比 ads 聚合 vs 报表展示值,记录误差(目标 <0.5%,对齐 sprint-29 对账风格)。
- 差异溯源:多半来自三级金额选列/含税/双重计数/别名(p_project_good_alias)——回写 §3 护栏或 mart 修正。

## 影响范围

- `it/sql/` 对账 SQL;`it/evidence/` 对账证据

## 验证

- [x] 选样对账 SQL 已成文,可在目标库重跑并登记误差
- [x] 误差源中的三级金额、含税、source_type=8 已回写 guardrail/mart

## 完成标准

- [x] 财务 ads 与 adminweb 内建报表对账 SQL 可重跑,误差登记由运行环境输出

## 证据

- `it/sql/f4_finance_adminweb_reconciliation.sql`
- `assets/finance-mart-catalog.md`
