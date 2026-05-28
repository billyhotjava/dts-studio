# Sprint-23 IT 验收入口

**范围**: 基于 Agent 的 PRS BI 报表，自然语言导报表闭环。  
**首个验收域**: 租赁/报花。  
**默认分析窗口**: `2025-05-01` 至当前日期。  
**当前状态**: 本地契约回归已脚本化；上线环境 20 条 Golden Questions 通过率由 `api-smoke` 或人工验收补证。

## 验收目标

1. 用户输入自然语言后，Planner 能稳定输出正确响应类型。
2. 固定报表/大屏类问句优先命中 L2 资产。
3. 趋势、排行、汇总类问句生成 `REPORT_DRAFT`。
4. 明细和当前状态类问句走 L0 只读或返回业务深链。
5. 报表草稿可预览、保存、创建图表并加入大屏。
6. 数据质量为 `MEDIUM` / `LOW` 的报表必须展示质量提示。
7. 动作类问句只生成 `ACTION_PROPOSAL`，不直接写业务系统。

## Golden Questions

完整验收集见：

```text
it/golden-questions-prs-agent-bi.md
```

覆盖 24 条问题：

| 类型 | 问句数 | 覆盖点 |
|---|---:|---|
| `FIXED_REPORT` | 6 | PRS 大屏 L2 快路径 |
| `REPORT_DRAFT` | 11 | 租赁趋势、坏账、待审批、养护、回收、变更、费用 |
| `BUSINESS_DETAIL` | 2 | 租赁账单/待确认事项 L0 只读 |
| `BUSINESS_INSIGHT` | 3 | 采购库存、任务巡检、审批待办 |
| `ACTION_PROPOSAL` | 2 | 催收提案，不执行写动作 |

## 验收脚本

```bash
bash worklog/v1.0.0/sprint-23-202605/it/test_prs_agent_bi_report.sh
```

脚本默认执行：

- `dts-copilot-ai` Planner / Chat / SSE metadata 单元测试。
- `dts-copilot-analytics` `analysis_draft` metadata 映射测试。
- `dts-copilot-webapp` Copilot 报表草稿与生成报表卡组件测试。

如需跑上线环境接口冒烟，额外设置：

```bash
DTS_COPILOT_BASE_URL=http://127.0.0.1:18082 \
DTS_COPILOT_COOKIE='portal_session=...' \
bash worklog/v1.0.0/sprint-23-202605/it/test_prs_agent_bi_report.sh
```

接口冒烟会输出：

- `responseKind`
- `dataSurface`
- `qualityLevel`
- `reportCode`
- 原始响应摘要

## 证据目录

```text
it/evidence/YYYYMMDD-local/
  run.log
  ai-tests.log
  analytics-tests.log
  webapp-copilot-tests.log
  api-smoke.jsonl
```

## 完成标准

- [x] 20 条以上 Golden Questions 已整理。
- [x] 本地契约测试覆盖响应类型、数据面、质量字段和草稿 metadata。
- [x] 验收脚本可复现执行后端、分析端和前端关键回归。
- [ ] 上线环境 20 条 Golden Questions 实测通过，并补充 API 响应或截图证据。
- [ ] 至少 3 条草稿在真实页面完成创建图表并加入大屏。
- [x] 动作类问句不出现写 SQL 或未确认业务动作执行。
