# BG-07: 意图路由规则引擎

**优先级**: P1
**状态**: READY
**依赖**: BG-02, BG-03

## 目标

在 Copilot 运行时增加规则引擎路由器，通过关键词匹配 + 简单分类器判定业务域和目标视图，不消耗 LLM 调用，减少延迟和路由错误。

## 设计变更说明

原方案（BG-07）使用 LLM 做意图路由（主题域识别 + 查询类型判断 + 通道选择）。修改原因：
- 多一次 LLM 调用增加 1-2s 延迟
- 路由错误会导致后续全错（错误放大效应）
- 业务域有限（6 个），关键词特征明显，规则引擎足够

## 技术设计

### 运行时处理流程

```
用户问句
  │
  ├─ Step 1: 预制模板匹配（BG-04）
  │    ├─ 命中 → 参数提取 → 执行模板 SQL
  │    └─ 未命中 ↓
  │
  ├─ Step 2: 规则路由
  │    ├─ 关键词提取 → 业务域判定 → 目标视图选择
  │    ├─ 低置信度（多域冲突 / 无匹配） → 澄清
  │    └─ 高置信度 ↓
  │
  └─ Step 3: NL2SQL 生成
       └─ 上下文 = 目标视图 DDL + 枚举 + 同义词 + few-shot
```

### 业务域路由规则

```java
// 路由规则表：keyword → domain → target_views
// 匹配逻辑：统计各域命中关键词数量，取最高分域

rules = [
  // 项目域
  { keywords: ["项目", "在服", "项目点", "合同", "客户", "签约", "到期"],
    domain: "project",
    primary_view: "v_project_overview",
    secondary_views: ["v_project_green_current"] },

  // 报花业务域
  { keywords: ["加花", "换花", "减花", "调花", "报花", "坏账", "售花", "业务单", "审批", "驳回"],
    domain: "flowerbiz",
    primary_view: "v_flower_biz_detail",
    secondary_views: [] },

  // 绿植域
  { keywords: ["绿植", "摆位", "在摆", "实摆", "花盆", "花架", "摆放"],
    domain: "green",
    primary_view: "v_project_green_current",
    secondary_views: ["v_project_overview"] },

  // 结算域
  { keywords: ["租金", "应收", "收款", "欠款", "结算", "未结算", "月租", "账单", "开票"],
    domain: "settlement",
    primary_view: "v_monthly_settlement",
    secondary_views: ["v_project_overview"] },

  // 任务域
  { keywords: ["任务", "待办", "待处理", "执行", "完成率", "进行中"],
    domain: "task",
    primary_view: "v_task_progress",
    secondary_views: ["v_pendulum_progress"] },

  // 养护域
  { keywords: ["养护", "养护人", "巡检", "覆盖率", "养护记录"],
    domain: "curing",
    primary_view: "v_curing_coverage",
    secondary_views: [] },

  // 初摆域
  { keywords: ["初摆", "首摆", "新项目布置", "预算"],
    domain: "pendulum",
    primary_view: "v_pendulum_progress",
    secondary_views: [] },
]
```

### 置信度判定

```
score = 命中关键词数量 / 问句总关键词数量

高置信度 (score >= 0.3 且单域最高):
  → 选择该域的 primary_view，进入 NL2SQL

中置信度 (0.15 <= score < 0.3 或两域分数接近):
  → 选择 top-2 域的 primary_view，都注入上下文，让 NL2SQL 自行选择

低置信度 (score < 0.15 或无命中):
  → 返回澄清问题：
    "您的问题可能涉及以下方面，请确认：
     1. 项目和客户信息
     2. 报花业务（加花/换花/减花）
     3. 租金和结算
     4. 任务进度
     5. 养护情况"
```

### 特殊路由规则

1. **结算类问题强制走已结算结果**：含"租金/应收/收款/结算"关键词时，只注入 `v_monthly_settlement`，不注入绿植视图（防止模型尝试从绿植重算租金）

2. **跨域问题处理**："XX项目的换花率" 同时命中 project 和 flowerbiz → 注入两个视图，让 NL2SQL 生成跨视图查询

3. **否定词处理**："未结算/未养护/未完成/待审批" → 自动识别为过滤条件，不影响域判定

### 实现位置

- 新建 `IntentRouter` 服务，在 `copilot-ai` 模块
- 路由规则外置为配置（数据库表或 YAML），支持热更新
- 与现有 `Nl2SqlService` 集成，在生成 prompt 前调用

### 路由规则配置表

```sql
CREATE TABLE nl2sql_routing_rule (
    id              BIGSERIAL PRIMARY KEY,
    domain          VARCHAR(32)   NOT NULL,
    keywords        TEXT          NOT NULL,   -- JSON 数组
    primary_view    VARCHAR(128)  NOT NULL,
    secondary_views TEXT,                     -- JSON 数组
    priority        INT DEFAULT 0,
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);
```

## 完成标准

- [ ] `IntentRouter` 服务实现
- [ ] 6+ 个业务域路由规则入库
- [ ] 置信度判定逻辑：高 → 直接路由，中 → 多视图注入，低 → 澄清
- [ ] 结算类问题强制走 `v_monthly_settlement`
- [ ] 路由规则支持热更新
- [ ] 路由准确率 > 90%（基于 BG-06 eval case 验证）
- [ ] 路由延迟 < 50ms（纯规则计算，无 LLM 调用）
