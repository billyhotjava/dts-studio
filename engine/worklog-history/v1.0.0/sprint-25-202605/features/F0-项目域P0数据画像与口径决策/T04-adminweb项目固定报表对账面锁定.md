# T04: adminweb 项目固定报表对账面锁定

**优先级**: P0  
**状态**: BLOCKED  
**依赖**: T02

## 目标

选定项目域 dbt ADS 的对账来源，确保 `xycyl_ads_project_*` 不是脱离现有业务系统的孤立口径。

## 阻塞

依赖 T02 识别出的可用 ODS 表和数据分布。当前只具备 `p_project` / `p_customer`，无法和项目实摆固定报表做有效对账。

## 候选对账面

- `FlowerSumMapper` 中项目实摆租金、成本、数量聚合。
- `ProjectSummaryMapper` 中项目统计汇总。
- adminweb `flower/project`、`flower/statistics` 下的项目总览页面。

## 验证

- [ ] 固定报表 SQL 或 Mapper 方法定位到文件和行号。
- [ ] 记录输入条件和预期指标：项目数、实摆数量、租金、成本。
- [ ] 明确误差门槛：关键金额误差 < 0.5%。

## 完成标准

- [ ] F1/F3 有可复现对账基准。
