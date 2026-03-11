# DTS Agent Protocol (DAP)

> Studio、dts-stack、app-stack 三者交互的标准协议
> 产品的核心护城河 — 定义了 AI Agent 如何理解物理世界并执行决策

## Why DAP Is The Moat

传统软件的护城河是代码量和功能复杂度。DTS 的护城河不是代码，而是**协议**。

- 代码可以被重写，协议定义的生态不能
- 每个行业 Pack 遵循 DAP 接入，形成网络效应
- 客户的 Ontology 和 Skill 资产建立在 DAP 之上，迁移成本 = 丢失核心竞争力
- DAP 是 AI Agent 理解企业物理世界的"语言标准"

类比：
- Android 的护城河不是 AOSP 代码，而是 Google Play 生态协议
- Palantir 的护城河不是 Foundry 代码，而是 Ontology 数据模型标准
- DTS 的护城河是 **DAP — AI Agent 与物理世界的交互协议**

## Protocol Layers

```
Layer 5: Audit Protocol        — 决策可追溯性保障
Layer 4: Security Protocol     — 身份与权限在 Agent 间传递
Layer 3: Intent Protocol       — 自然语言 → 结构化意图 → 执行计划
Layer 2: Skill Protocol        — AI 技能的定义、注册、调用、编排
Layer 1: Ontology Protocol     — 物理世界 → 数字映射的标准格式
Layer 0: Transport             — gRPC (control) + Kafka (data) + SSE (streaming)
```

---

## Layer 0: Transport

基础通信层。不是 DAP 的创新，但是 DAP 的基础。

```
Agent ←→ Agent:     gRPC (protobuf)
Agent  → Platform:  gRPC (with JWT metadata)
Agent  → Frontend:  SSE (streaming responses)
Agent  → Audit:     Kafka (async events)
Pack   → Platform:  gRPC via dts-gateway (enforced auth)
```

---

## Layer 1: Ontology Protocol

**定义 AI Agent 如何理解客户的物理世界。**

### ObjectType — 业务对象类型

```yaml
# 每个行业的核心实体都用这个格式定义
apiVersion: dap/v1
kind: ObjectType
metadata:
  name: Machine
  namespace: manufacturing    # 行业命名空间
  description: 生产设备
spec:
  properties:
    - name: machine_id
      type: string
      required: true
      classification: internal    # 数据安全分级
    - name: status
      type: enum
      values: [running, idle, maintenance, fault]
    - name: location
      type: reference
      target: ProductionLine
    - name: temperature
      type: number
      unit: celsius
      source: scada             # 数据来源标记
      frequency: realtime
  actions:                      # 该对象支持的操作
    - name: start
      risk_level: high          # 触发 HITL 审批
    - name: stop
      risk_level: high
    - name: schedule_maintenance
      risk_level: medium
```

### Relationship — 关系定义

```yaml
apiVersion: dap/v1
kind: Relationship
metadata:
  namespace: manufacturing
spec:
  - from: Machine
    to: ProductionLine
    type: belongs_to
    cardinality: many_to_one
  - from: QualityInspection
    to: Machine
    type: inspects
    cardinality: many_to_one
  - from: WorkOrder
    to: Machine
    type: targets
    cardinality: many_to_one
```

### Metric — 指标定义

```yaml
apiVersion: dap/v1
kind: Metric
metadata:
  name: oee
  namespace: manufacturing
  description: Overall Equipment Effectiveness
spec:
  formula: availability * performance * quality
  unit: percent
  frequency: hourly
  thresholds:
    warning: 85
    critical: 70
    direction: below    # 低于阈值触发告警
  dependencies:
    - Machine.status
    - Machine.cycle_time
    - QualityInspection.result
```

### 关键设计原则

1. **声明式而非命令式** — 定义"是什么"而非"怎么做"，Agent 自主决定如何查询和操作
2. **数据安全内置** — 每个属性标记 classification，Agent 读取时自动过 dts-data-security
3. **来源标记** — 标记 source 和 frequency，Agent 知道去哪拿数据（SCADA 实时 vs ERP 批量）
4. **行业命名空间** — 防止跨 Pack 名称冲突，支持多 Pack 共存

---

## Layer 2: Skill Protocol

**定义 AI 能力的标准接口。专家经验通过这个协议变成可执行的系统能力。**

### Skill Descriptor — 技能声明

```yaml
apiVersion: dap/v1
kind: Skill
metadata:
  name: predict-maintenance
  namespace: manufacturing
  description: 基于 SCADA 趋势和历史维保预测设备故障
spec:
  type: hybrid                  # llm / rule / hybrid / workflow
  risk_level: low               # low / medium / high
  permissions:
    - data:read:machine
    - data:read:maintenance
  input:
    schema:
      type: object
      properties:
        machine_id:
          type: string
        lookback_days:
          type: integer
          default: 30
  output:
    schema:
      type: object
      properties:
        failure_probability:
          type: number
        predicted_date:
          type: string
          format: date
        recommended_action:
          type: string
  endpoint: grpc://manufacturing-pack:50051/skill/predict-maintenance
  evaluation:
    suite: eval/predict-maintenance.jsonl
    baseline_accuracy: 0.82
```

### Skill Invocation — 调用协议

```protobuf
// 标准 gRPC 接口 — 所有 Skill 必须实现
service SkillService {
  // 执行技能 (streaming for progress)
  rpc Execute(SkillRequest) returns (stream SkillResponse);
  // 描述技能 (Agent 用于 tool selection)
  rpc Describe(DescribeRequest) returns (SkillDescriptor);
  // 预校验 (dry-run)
  rpc Validate(ValidateRequest) returns (ValidateResponse);
}

message SkillRequest {
  string skill_name = 1;
  string trace_id = 2;           // 链路追踪
  RequestContext context = 3;     // 用户身份 + 权限
  google.protobuf.Struct input = 4;
}

message SkillResponse {
  enum Status { RUNNING, COMPLETED, FAILED, AWAITING_APPROVAL }
  Status status = 1;
  string step_description = 2;   // 当前步骤描述 (streaming)
  google.protobuf.Struct output = 3;
  AuditEntry audit = 4;          // 每步审计记录
}

message RequestContext {
  string user_id = 1;
  string tenant_id = 2;
  string jwt_token = 3;
  repeated string permissions = 4;
  string trace_id = 5;
}
```

### 关键设计原则

1. **描述即发现** — Agent 通过 `Describe()` 获取所有可用 Skill 的元数据，LLM Function Calling 自动选择
2. **权限前置** — `RequestContext` 在调用时传递，Skill 内部必须校验
3. **流式响应** — Agent 的多步推理过程实时推送给前端（SSE）
4. **审计内嵌** — 每个 `SkillResponse` 携带审计条目，不依赖 Skill 开发者额外实现

---

## Layer 3: Intent Protocol

**定义自然语言如何转化为结构化的决策意图。**

### Intent Structure

```yaml
apiVersion: dap/v1
kind: Intent
metadata:
  trace_id: "abc-123"
  user_id: "user-42"
  source: pilot-webapp          # 来源端
spec:
  # 原始输入
  raw_input: "帮我看看A3产线今天的OEE，如果低于80%就安排维保"

  # 解析后的结构化意图
  goals:
    - type: query
      target: Metric/oee
      filter:
        production_line: A3
        time_range: today
    - type: conditional_action
      condition: "oee < 0.8"
      action: schedule_maintenance
      target: Machine
      filter:
        production_line: A3

  # 执行计划 (Agent 生成)
  plan:
    steps:
      - step: 1
        skill: data/execute-query
        input: { query: "SELECT oee FROM metrics WHERE line='A3' AND date=TODAY" }
      - step: 2
        skill: mfg/calculate-oee
        input: { production_line: "A3" }
        depends_on: [1]
      - step: 3
        condition: "step_2.output.oee < 0.8"
        skill: action/create-workflow
        input: { type: "maintenance", target: "A3 machines" }
        risk_level: medium
        depends_on: [2]

  # 约束
  constraints:
    max_steps: 10
    timeout_seconds: 120
    require_approval_above: medium    # medium 以上需 HITL
```

### Intent Lifecycle

```
用户输入 (NL)
  → dts-intent-engine: 解析为 Intent 结构
  → dts-agent: 生成 Plan (步骤编排)
  → 逐步执行 Skills
  → 遇到 high risk → 暂停 → dts-workflow HITL 审批
  → 审批通过 → 继续执行
  → 结果 → SSE 流式返回前端
  → 审计 → Kafka → dts-audit-log + dts-observability
```

### 关键设计原则

1. **意图与执行分离** — Intent 描述"要什么"，Plan 描述"怎么做"，Agent 自主规划
2. **条件执行** — 支持 if-then 逻辑，一句话完成多步决策链
3. **风险分级** — 每步自动评估风险，高风险自动暂停等人工确认
4. **可解释** — 每个 Intent 的 Plan 可被用户查看和理解

---

## Layer 4: Security Protocol

**定义 Agent 间如何安全地传递身份和权限。**

### Identity Propagation

```
Frontend (JWT from Keycloak)
  → dts-gateway (验证 JWT, 提取 user_id/tenant_id/roles)
  → gRPC metadata (RequestContext)
  → 每个服务 (校验 permissions against dts-platform)
  → dts-data-security (按 user 权限过滤数据)
  → Skill 执行 (同一 RequestContext 传递)
  → AppPack Skill (同样的权限校验，无特权)
```

### Data Security Envelope

```protobuf
// 每次数据出口都经过这个检查
message DataSecurityCheck {
  string user_id = 1;
  string tenant_id = 2;
  repeated string requested_columns = 3;
  string target_object_type = 4;    // Ontology ObjectType

  // 返回
  repeated ColumnPolicy policies = 5;  // allow / mask / deny per column
  string row_filter_sql = 6;           // 行级过滤条件
}
```

### 关键设计原则

1. **零信任** — 每次调用都验证，不缓存权限决策
2. **数据安全在 Agent 链路内** — 不是事后审计，而是实时拦截
3. **AppPack 无特权** — Pack 的 Skill 看到的数据 = 当前用户有权看到的数据

---

## Layer 5: Audit Protocol

**定义 AI 决策的完整可追溯性。**

### Audit Event Schema (CloudEvents)

```json
{
  "specversion": "1.0",
  "id": "evt-001",
  "source": "dts-agent",
  "type": "dap.skill.executed",
  "time": "2026-03-11T10:30:00Z",
  "subject": "trace-abc-123",
  "data": {
    "user_id": "user-42",
    "tenant_id": "tenant-1",
    "intent_id": "intent-789",
    "skill_name": "mfg/calculate-oee",
    "step": 2,
    "input_summary": "production_line=A3",
    "output_summary": "oee=0.76",
    "data_accessed": ["metrics.oee", "machine.status"],
    "duration_ms": 1200,
    "token_cost": 150,
    "risk_level": "low",
    "approval_status": null
  }
}
```

### Audit Event Types

```
dap.intent.parsed       — 意图解析完成
dap.plan.generated      — 执行计划生成
dap.skill.started       — 技能开始执行
dap.skill.executed      — 技能执行完成
dap.skill.failed        — 技能执行失败
dap.approval.requested  — HITL 审批请求
dap.approval.granted    — 审批通过
dap.approval.rejected   — 审批拒绝
dap.data.accessed       — 数据访问记录
dap.session.completed   — 整个决策链完成
```

### 关键设计原则

1. **不可篡改** — Kafka → dts-audit-log append-only 存储
2. **全链路** — 从用户输入到最终执行，每一步都有记录
3. **AI 透明** — Agent 看了什么数据、做了什么推理、为什么选这个 Skill，全部记录
4. **成本追踪** — 每次 LLM 调用的 token 成本记录，支持用量计费

---

## Protocol Versioning

```
dap/v1  — 初始版本 (当前)
dap/v2  — 预留：多模态 Intent (语音/视觉输入)
dap/v3  — 预留：跨组织 Agent 协作 (Agent-to-Agent)
```

协议版本在每个资源的 `apiVersion` 字段声明。向后兼容：v2 的 Agent 能处理 v1 的资源。
