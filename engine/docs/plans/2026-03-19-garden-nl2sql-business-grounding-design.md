# 园林业务 NL2SQL 业务 Grounding 与双通道架构设计

## 背景

`adminapi + adminweb` 的主业务代码已经明确呈现出两条优先主题域：

- 项目履约：项目、客户、合同、点位、项目绿植、养护/履约记录
- 现场业务：加花、减花、换花、转移、回收、初摆/实摆、任务执行

`dts-copilot` 当前已经具备以下底座：

- 外部业务库数据源注册与元数据同步
- `analytics_table / analytics_field / analytics_metric / analytics_synonym`
- `Nl2SqlSemanticRecallService`、NL2SQL eval case / eval run
- AI 聊天、schema lookup、SQL 执行、数据源透传

但现状仍然偏“技术可用”，距离面向业务系统的产品级 NL2SQL 还差一层稳定的业务认知与执行约束。

## 设计目标

1. 让 Copilot 对项目履约、现场业务两个主题域形成稳定的业务认知。
2. 在保持 Phase 1 直连业务库能力的同时，为后续轻量 ELT 主题层留出清晰演进路径。
3. 将业务理解沉淀为结构化语义资产，而不是长期堆积在 prompt 和零散 Tool 中。
4. 将 SQL 生成与 SQL 执行分层，最终由 `analytics` 侧承担权限、执行、缓存和审计。

## 非目标

- 本阶段不做全量企业数仓重构。
- 本阶段不做跨多个业务库的联邦 JOIN。
- 本阶段不做向量化 schema recall 作为主路径。
- 本阶段不把财务域作为第一优先语义包。

## 核心判断

### 1. 不能继续依赖“模型猜业务”

仅靠 schema lookup + 通用 prompt，对 `adminapi` 这种历史业务库不够稳。问题不只在于找表找字段，还在于：

- 业务词和库表命名长期不一致
- 指标口径分散在业务代码和历史认知里
- 同一个问题既可能需要明细查询，也可能需要指标层回答

因此必须显式建设业务 Grounding。

### 2. 双通道是当前最适合的路线

- 明细追问、现场追踪：继续走业务只读库直连
- 稳定指标、趋势分析、跨流程聚合：走轻量 ELT 后的主题层

这与现有 `dts-copilot` 文档中 “Phase 1 直连业务库、Phase 2 ELT” 的演进方向一致。

## 业务认知资产

### 1. 业务对象层

沉淀业务对象字典，明确：

- 对象名称：项目、项目点、客户、合同、点位、项目绿植、日常任务、执行任务、加花事件、换花事件、回收事件、初摆单
- 所属主题域
- 关联的源表集合
- 主时间字段、主状态字段、主组织字段

该层优先复用：

- `analytics_table.display_name`
- `analytics_table.description`
- 补充的业务对象字典表

### 2. 关系与 Join Contract 层

定义受控的 join 关系，而不是让模型自由拼接全库：

- 允许表集合
- 主键 / 外键
- 合法 join 路径
- 默认时间字段
- 常用筛选字段

这层是 NL2SQL 准确率和安全性的关键资产。

### 3. 指标与口径层

将高频指标沉淀到统一指标定义：

- 项目履约域：在服项目数、活跃项目数、点位数、在场绿植数、履约任务完成率
- 现场业务域：加花次数、换花数量、回收数量、初摆完成数、异常任务数

优先复用 `analytics_metric.metric_json` 作为 v1 的指标存储载体。

### 4. 同义词与问句样本层

围绕两个主题域沉淀：

- 同义词：项目点=项目、实摆=初摆/摆放、加花=增补、养护=履约任务
- 问句样本：高频中文问法到标准 SQL/指标模板
- eval case：按主题域持续回归

这层优先复用：

- `analytics_synonym`
- `analytics_nl2sql_eval_case`
- `Nl2SqlSemanticRecallService`

## 双通道运行时架构

### 步骤 1：问题路由

用户提问后，先判定：

- 主题域：项目履约 / 现场业务 / 其他
- 查询类型：明细追问 / 指标统计 / 趋势分析 / 诊断解释

### 步骤 2：通道选择

- 明细追问优先走 `Realtime Query` 直连通道
- 指标统计、趋势分析优先走 `Semantic Mart` 主题层
- 若问题置信度不足，则先澄清，不直接生成 SQL

### 步骤 3：上下文编译

给模型的不是整库，而是一份编译后的上下文包：

- `domain`
- `intent`
- `business_objects`
- `allowed_tables`
- `allowed_joins`
- `metrics`
- `time_hints`
- `synonyms`
- `examples`
- `scope_constraints`

### 步骤 4：执行分层

- `ai` 负责理解问题、生成候选 SQL 或查询计划
- `analytics` 负责权限校验、查询编排、缓存、执行和审计

长期不建议保留 AI 侧“直接 JDBC 执行最终结果”的产品主路径。

## 轻量 ELT 平台

### 目标

不引入完整数仓平台，先在 `analytics` 内部落一个轻量增量同步能力。

### v1 主题层

先只建设两个主题层对象：

- `mart_project_fulfillment_daily`
- `fact_field_operation_event`

### 同步策略

- 以小时级或更短频率的增量同步为主
- 优先基于业务时间字段或自增主键做 watermark
- 保持 “只补两个主题域” 的约束，不做全库搬运

### 演进路径

- v1：内嵌式轻量 ELT，同步到 `copilot_analytics`
- v2：增量任务、预聚合视图、更多主题层
- v3：再考虑 dbt / Airflow 化

## 权限与安全

1. AI 侧继续保留只读 SQL 沙箱。
2. 查询权限、组织范围、表访问范围必须在 `analytics` 侧统一收口。
3. 直连通道也必须经过 `allowed_tables + join_contract + permission_bridge` 约束。
4. 主题层默认只暴露已经完成语义编译和权限映射的字段。

## 交付顺序

本设计对应 `worklog/v1.0.0/sprint-10`，任务顺序为：

1. 业务域盘点与语义源映射
2. 语义对象/字段/关系模型基线
3. 项目履约语义包
4. 现场业务语义包
5. Join Contract 与 Allowed Tables 编译
6. 指标口径与 Metric Store 对齐
7. 意图路由与双通道判定
8. 直连通道上下文编译与权限桥接
9. 轻量 ELT 主题层与增量同步
10. IT 集成测试与验收矩阵

## 成功标准

- Copilot 在两个优先主题域中不再依赖“全库猜表”
- 高价值问句能稳定落到正确的表集合和 join 路径
- 指标类问题优先走统一口径
- 明细类问题优先走受控直连查询
- `adminweb -> copilot -> analytics -> 业务库` 形成可重复的集成测试链
