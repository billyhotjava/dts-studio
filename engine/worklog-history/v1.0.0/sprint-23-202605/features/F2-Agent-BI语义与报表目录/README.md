# F2: Agent BI 语义与报表目录

**优先级**: P0  
**状态**: DONE

## 目标

把 F1 的业务域资产变成 Agent 能执行的报表目录和路由规则。Agent 先判断“已有资产能否回答”，再决定是否生成新 SQL。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | Agent BI 报表目录与语义包 | P0 | DONE | F1-T01 |
| T02 | L0/L1/L2 数据面路由与质量等级 | P0 | DONE | T01 |
| T03 | `REPORT_DRAFT` 协议与草稿落点 | P0 | DONE | T01, T02 |

## 完成标准

- [x] 每个报表目录项明确业务域、问题模板、数据面、质量等级、展示建议。
- [x] Planner 默认优先顺序为：固定报表/大屏 -> dbt ADS/DWS -> adminapi 只读 -> 受控探索。
- [x] `REPORT_DRAFT` 输出可以被前端渲染，也可以落 `analysis_draft`。
- [x] 动态 SQL 只允许访问语义包白名单。
