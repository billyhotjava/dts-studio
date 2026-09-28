# T01: NL2SQL 黄金集样例库

**优先级**: P1
**状态**: DONE
**依赖**: F1

## 目标

建立可扩展黄金集，记录自然语言、预期模板/指标、目标数据层、核心 SQL 片段和最低可信等级。

## 技术设计

- 样例字段：`question`、`expectedTemplate`、`expectedTarget`、`expectedGradeAtLeast`、`requiredEvidence`。
- 首批样例：
  - `2026年凭证的数据统计下` -> `TPL-57` -> ADS voucher。
  - `2026年财务数据统计下` -> `TPL-56` -> ADS finance。
  - 报花订单月度查询 -> 受控模板/ADS 或明确弱路径。

## 影响范围

- `dts-copilot-ai/src/main/resources/governance/nl2sql-accuracy-golden-set.v1.json`。
- route/template matcher tests。
- IT 脚本。

## 落地结果

- 已落地 `Nl2SqlAccuracyGoldenSetRegistry`，从 governance JSON 读取启用样例并拒绝重复 ID。
- 首批覆盖 `2026年凭证的数据统计下`、`2026年财务数据统计下`、`本月报花订单明细看下`。
- 每条样例声明预期模板、目标 relation、数据层、最低可信等级、SQL 必含/禁用片段和必须证据。

## 验证

- [x] 黄金集样例能批量执行。
- [x] 任何样例落到 L0 profile 时测试失败。
- [x] 任何样例 target 变成应用 MySQL 时测试失败。

## 完成标准

- [x] 黄金集成为新增模板和场景接入的必过门禁。
