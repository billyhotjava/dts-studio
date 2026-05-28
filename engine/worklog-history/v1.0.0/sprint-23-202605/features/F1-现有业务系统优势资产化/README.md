# F1: 现有业务系统优势资产化

**优先级**: P0  
**状态**: DONE

## 目标

把 `adminapi`、`adminweb`、`dts-copilot` 已有能力整理成 Agent BI 可消费的业务域资产。核心不是罗列代码，而是回答三个问题：

1. 现有系统哪些业务能力对 Agent BI 最有价值？
2. 每个业务域最适合生成什么报表？
3. 每类问题应该走固定报表、dbt 主题表，还是 adminapi 只读接口？

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 现有系统优势与业务域资产图 | P0 | DONE | sprint-22 报花资产 |
| T02 | 业务问句分类与意图词典 | P0 | DONE | T01 |

## 完成标准

- [x] 至少 5 个 PRS 业务域被整理成 Agent BI 资产。
- [x] 每个业务域有业务对象、页面/API 证据、典型问句、首选取数面、数据质量等级。
- [x] 业务问句被映射到 `FIXED_REPORT`、`REPORT_DRAFT`、`BUSINESS_DETAIL`、`BUSINESS_INSIGHT`、`ACTION_PROPOSAL`。
- [x] 后续 F2 可直接基于该资产图生成报表目录和 Planner 规则。
