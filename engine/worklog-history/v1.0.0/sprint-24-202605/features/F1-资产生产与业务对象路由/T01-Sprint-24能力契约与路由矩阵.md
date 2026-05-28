# T01: Sprint-24 能力契约与路由矩阵

**优先级**: P0  
**状态**: DONE  
**依赖**: 无

## 目标

明确“报表资产生产器 + 业务对象问答器”的能力边界和首层路由规则。

## 技术设计

Agent 首层路由固定为：

1. `L2_FIXED_REPORT`：已有固定报表、大屏、模板。
2. `L1_DBT_MART`：已有 ADS/DWS 主题表。
3. `L1_CANDIDATE_ADS`：没有 ADS 时生成候选 dbt 模型草稿。
4. `L0_BUSINESS_OBJECT`：页面/接口/ODS 对应的业务对象问答。
5. `ACTION_PROPOSAL`：反向指导业务系统，只生成提案，不写业务。

## 影响范围

- `dts-copilot-ai` Planner、语义包、报表目录、业务对象目录。
- `dts-copilot-analytics` 后续保存候选 ADS/业务对象画像。
- `dts-copilot-webapp` 后续展示业务对象卡、候选 ADS 卡。

## 验证

- [x] Sprint 文档描述每个数据面和非目标。
- [x] Feature/Task 明确第一步实现边界。

## 完成标准

- [x] 新 sprint 目录、Feature、Task 和 queue 条目可追踪。

