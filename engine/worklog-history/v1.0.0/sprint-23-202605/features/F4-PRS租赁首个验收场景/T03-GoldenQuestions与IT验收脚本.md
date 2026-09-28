# T03: Golden Questions 与 IT 验收脚本

**优先级**: P0  
**状态**: DONE  
**依赖**: T01, T02

## 目标

建立 Sprint-23 的可复现验收集，避免自然语言导报表只靠人工主观判断。

## 输出物

- `it/golden-questions-prs-agent-bi.md`
- `it/test_prs_agent_bi_report.sh`
- `it/evidence/<date>/` 验收证据目录。

## 验收维度

| 维度 | 要求 |
|---|---|
| 路由 | 问句正确落到固定报表、报表草稿或业务明细 |
| SQL 安全 | 只允许 SELECT，且访问白名单 |
| 数据质量 | `qualityLevel` 和 `qualityNotes` 正确展示 |
| 可视化 | 展示类型与字段匹配 |
| 资产化 | 草稿可保存，card/screen 可创建 |
| 追溯 | 报表保留原始问句、数据面、SQL 和质量提示 |

## 完成标准

- [x] Golden Questions 至少 20 条。
- [x] IT 脚本能自动检查响应类型、SQL 安全和质量字段。
- [ ] 人工验收记录包含截图或 API 响应样例。
