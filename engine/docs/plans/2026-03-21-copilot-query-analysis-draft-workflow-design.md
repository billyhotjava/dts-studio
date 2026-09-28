# Copilot 与查询资产中心协同设计

## 背景

当前 `dts-copilot` 中，菜单页和 `AI Copilot` 是两个平级入口，但二者之间的资产衔接仍然偏弱：

- `Copilot` 产出的大多还是聊天消息和 URL 级 SQL 跳转
- `查询` 页面本质上仍偏正式 Query/Card 管理
- 用户从对话到查询编辑，再到可视化、仪表盘、大屏的链路不够连贯
- 临时探索结果和正式分析资产没有清晰分层

这会带来两个直接问题：

1. `Copilot` 结果容易沦为一次性答案，难以沉淀
2. `查询` 目录会被半成品污染，正式资产管理体验变差

## 设计目标

1. 保持 `查询` 菜单页与 `Copilot` 平级，不改变导航主结构
2. 让 `Copilot` 生成的查询结果先沉淀为 `分析草稿`
3. 让 `查询` 页面升级为“分析资产中心”，统一管理草稿与正式查询
4. 打通 `Copilot -> 草稿 -> 正式查询 -> 可视化 -> 仪表盘/大屏/报告` 的晋升链
5. 明确区分临时探索资产与正式分析资产，避免目录污染

## 非目标

- 本阶段不重写现有 Query/Card 领域模型
- 本阶段不改变菜单导航层级
- 本阶段不让 Copilot 直接批量创建正式查询卡片
- 本阶段不建设复杂审批流

## 核心判断

### 1. 平级入口是对的，但资产必须同源

用户既可以从左侧菜单进入 `查询`，也可以从右侧 `Copilot` 开始分析。  
这两个入口应保持平级，但必须共享同一套分析资产工作流。

### 2. Copilot 不应直接创建正式查询

若聊天侧一步到位生成正式 Query/Card：

- 正式目录会快速被试验性查询污染
- 用户很难分辨哪些是可复用资产，哪些只是临时探索
- “查询” 页面会退化成聊天历史的垃圾桶

因此必须引入中间层：`analysis_draft`

### 3. 查询页面应升级为分析资产中心

当前“查询”更像 Query/Card 管理页。  
设计上应升级为：

- 正式查询
- Copilot 草稿
- 最近分析

统一承接人工创建与 AI 生成的分析结果。

## 工作流设计

```text
菜单查询入口 / Copilot 入口
  -> Analysis Draft
      -> 试跑
      -> 编辑
      -> 保存为正式查询
      -> 创建可视化
          -> 加入仪表盘
          -> 加入大屏
          -> 进入报告工厂
```

## 状态模型

建议统一成五个状态：

1. `CONVERSATION_ONLY`
   - 仅存在于 Copilot 对话里
2. `DRAFT`
   - 已经形成结构化查询草稿
3. `SAVED_QUERY`
   - 已保存为正式查询卡片
4. `VISUALIZED`
   - 已生成图表
5. `PUBLISHED`
   - 已被加入仪表盘、大屏或报告工厂

## 信息架构

### 导航层

保持现有两个入口：

- `查询`
- `AI Copilot`

不新增一级菜单“草稿”。

### 查询页内部视图

建议在 `查询` 页面增加筛选视图：

- 全部
- 正式查询
- Copilot 草稿
- 最近分析

### Copilot 动作区

对 SQL / 报表型回答，动作区统一为：

- `执行查询`
- `保存草稿`
- `在查询中打开`
- `创建可视化`

## 对象模型

新增轻量对象：`analysis_draft`

建议字段：

- `id`
- `title`
- `source_type`：`copilot | manual`
- `session_id`
- `message_id`
- `question`
- `database_id`
- `sql_text`
- `explanation_text`
- `suggested_display`
- `status`
- `linked_card_id`
- `linked_dashboard_id`
- `linked_screen_id`
- `created_by`
- `created_at`
- `updated_at`

## API 设计

建议在 `analytics` 侧新增：

- `POST /api/analysis-drafts`
- `GET /api/analysis-drafts`
- `GET /api/analysis-drafts/{id}`
- `POST /api/analysis-drafts/{id}/run`
- `POST /api/analysis-drafts/{id}/save-card`
- `POST /api/analysis-drafts/{id}/create-visualization`
- `POST /api/analysis-drafts/{id}/archive`
- `DELETE /api/analysis-drafts/{id}`

这些接口归属于分析资产层，不属于聊天消息层。

## 页面交互细节

### Copilot 到查询页

从 `Copilot` 打开到查询页时，页面顶部显示来源条：

- 来源：`AI Copilot`
- 原始问题
- 数据源
- 当前状态：草稿
- 动作：
  - 返回对话
  - 保存正式查询
  - 创建可视化

### 查询列表项

每条查询/草稿显示：

- 标题
- 来源：手工 / Copilot
- 数据源
- 最近运行时间
- 状态：草稿 / 已保存 / 已图表化
- 原始问题摘要（若来自 Copilot）

## 多入口晋升链

一份 `analysis_draft` 应支持：

- 在查询页继续编辑
- 转为正式 Query/Card
- 进入图表创建
- 进入 Dashboard/Screen/Report Factory

避免每个入口各自维护一套临时对象。

## 实施顺序

### Phase 1

- 新增 `analysis_draft` 模型与接口
- 支持 Copilot 保存草稿
- 查询页支持草稿列表

### Phase 2

- 查询页承接 Copilot 来源上下文
- 支持草稿试跑、编辑、转正式查询

### Phase 3

- 打通草稿到可视化、仪表盘、大屏、报告工厂
- 增加 IT 验收和性能基线

## 验收标准

1. Copilot 生成的 SQL 类结果可保存为草稿
2. 查询页面可查看和管理 Copilot 草稿
3. 草稿不会污染正式查询目录
4. 草稿可转为正式查询
5. 草稿可进入可视化和多端复用链路
6. 页面能够明确显示资产来源与当前状态
