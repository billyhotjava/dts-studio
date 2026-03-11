# DTS Knowledge Strategy

> 基于 Polanyi 认识论 + LeCun 多模态预训练研究的知识战略框架
> 理论基础: Personal Knowledge (1958) + Beyond Language Modeling (2026)

## Core Thesis

DTS 的核心竞争力不是代码，而是**对物理世界的认知深度**。

- Polanyi: "We know more than we can tell" — 隐性知识是一切知识的根基
- LeCun: "Language = shadows on the wall" — 语言只是物理世界的投影
- DTS: Ontology + World Model = 比语言更深的物理世界理解

## 1. 知识金字塔 (Five-Layer Knowledge Pyramid)

```
Layer 4: 世界模型 (World Model)
  Agent 能预测操作后果: State + Action → Next State
  LeCun 发现: 通用预训练 + 1% 领域数据即可涌现
  ──────────────────────────────────────────────
Layer 3: 隐性模式 (Tacit Patterns)
  专家没说但一直在用的规则
  从多模态行为数据中统计推断
  Polanyi: subsidiary awareness
  ──────────────────────────────────────────────
Layer 2: 潜在知识 (Latent Knowledge)
  专家能说但没人问过的判断逻辑
  Gen-Ba 碎片化提取 (决策瞬间自然追问)
  ──────────────────────────────────────────────
Layer 1: 显性结构 (Explicit Structure)
  数据字典、行业标准、系统表结构
  传统 Ontology 建模 — 必要但不充分
  ──────────────────────────────────────────────
Layer 0: 多模态感知 (Multimodal Perception)
  视觉 + 声音 + 时序 + 文本
  LeCun: 视觉数据比文本更数据饥渴 (0.63 vs 0.53)
  Polanyi: 身体化知识 (embodied knowledge)
```

## 2. KnowHow 提取流水线 (Polanyi-Informed)

超越 Nonaka SECI 模型的局限（外化被过度简化、缺乏实证），采用 Gen-Ba 碎片策略。

### Phase 1: 现场观察 (Socialization)
- 专家在 Pilot 端做日常决策
- AI Agent 静默观察: 查看了哪些数据、做了什么判断、花了多长时间、采取了什么行动
- 不干扰专家工作流

### Phase 2: 碎片化外化 (Fragmented Externalization)
- 不要求专家"系统性描述"
- 在决策瞬间自然追问: "您刚才为什么选择提前维保？"
- 输出知识碎片: 容许不完整、容许矛盾、标记置信度
- 关键: 保持专家在 focal awareness，不破坏 subsidiary awareness

### Phase 3: 松散组合 (Loose Combination)
- LLM 将知识碎片聚类、去矛盾、补全
- 生成 KnowHow 草稿 (status: draft)
- 关联到 Ontology ObjectType
- 不追求完美，追求"够用的近似"

### Phase 4: 协作内化 (Collaborative Internalization)
- KnowHow 转化为 Skill (status: testing)
- 在真实场景中与专家并行运行
- 专家校正 AI 输出 → 反馈循环
- 通过 evaluation suite 量化准确率
- 达到 baseline_accuracy → status: active

### 与 SECI 的关键区别

| 维度 | SECI (Nonaka) | DTS 方法 |
|------|---------------|----------|
| 外化方式 | 系统性访谈、研讨会 | 决策瞬间的自然追问 |
| 完整性要求 | 追求完整的知识转化 | 容许碎片和不完整 |
| AI 角色 | 无 | LLM 做碎片组合和模式推断 |
| 验证方式 | 专家审核文档 | 并行运行 + 评测量化 |
| 对隐性知识的态度 | 能完全转化 | 只追求"够用的近似"，剩余靠 HITL |

## 3. 行业专家参与模式 (Indwelling-Driven)

Polanyi 的 indwelling 原则: 不能从外部理解一个领域，必须寓居其中。

### 反模式
```
顾问 → 访谈专家 → 写需求文档 → 开发 → 交付
                    ↑
              知识在这里变形 (Polanyi's paradox)
```

### 正确模式: 持续共创
```
专家日常使用 Pilot → AI 提出假设 → 专家校正 → KnowHow 积累
    → Skill 形成 → 与专家并行运行 → 持续校正 → Ontology 演进
```

### 三种专家角色

| 角色 | 参与方式 | 知识层级 |
|------|----------|----------|
| 日常使用者 | 通过 Pilot 正常工作，行为被 AI 学习 | Layer 0-2 |
| 知识审核者 | 审核 AI 生成的 KnowHow 草稿 | Layer 2-3 |
| 领域守护者 | 定义 Ontology 边界和不变量 | Layer 3-4 |

## 4. Ontology 作为"信念框架"

Polanyi: "Tacit assent and intellectual passions, the sharing of an idiom and of a cultural heritage, affiliation to a like-minded community shape our vision of the nature of things."

### 战略含义

1. **权威性来自共同体** — Ontology Council 机制，行业 Pack 的 Ontology 修改需专家评审
2. **护城河在于寓居深度** — 竞争对手能抄 YAML 格式，抄不走客户积累的 KnowHow 和 confidence 数据
3. **不追求完美映射** — 分层准确率衡量 (L1: ~95%, L2: ~75%, L3: ~60%, L4: TBD)

## 5. 物理世界孪生准确率框架

```
孪生分数 = Σ(layer_weight × layer_accuracy)

Layer 1 (显性结构): weight=0.25, target=95%  — 可验证
Layer 2 (潜在知识): weight=0.30, target=75%  — 需专家确认
Layer 3 (隐性模式): weight=0.25, target=60%  — 需长期运行数据
Layer 4 (世界模型): weight=0.20, target=TBD  — Phase 3 引入

残余不确定性由 HITL 覆盖 (铁律 1: 人工随时可接管)
```

## 6. 多模态 Ontology 演进路线 (LeCun-Informed)

### DAP v1 (当前): 纯结构化
- ObjectType YAML 定义
- 属性、关系、指标
- 文本/数值数据

### DAP v2 (预留): 多模态扩展
- ObjectType 增加 representations 声明 (visual, audio, timeseries)
- Skill 支持多模态输入/输出
- 视觉表征使用语义编码器 (RAE, 非 VAE — LeCun 论文结论)

### DAP v3 (远期): 世界模型
- ObjectType 增加 world_model 声明 (state_space, action_space)
- Agent 能在模型中"模拟运行"操作后果
- 跨组织 Agent 协作

## 7. 落地阶段

| 阶段 | 时间 | 重点 | 知识层级 |
|------|------|------|----------|
| Phase 1: 影子先行 | 0-6月 | 显性 Ontology + Intent + Skill 全链路 | L1 |
| Phase 2: 碎片积累 | 6-12月 | KnowHow 提取 + 多模态数据接入 | L0-L2 |
| Phase 3: 模式涌现 | 12-18月 | 隐性模式推断 + 世界模型涌现 | L3-L4 |
| Phase 4: 寓居深化 | 18月+ | 持续演进 + 跨客户知识迁移 | All |

## 8. 对 DAP 协议的补充建议

1. **tacit_level 标记** — KnowHow 标记来源层级 (explicit/latent/tacit)
2. **Ontology 演进日志** — 每个 ObjectType 变更来源 (标准导入/专家对话/AI推断)
3. **置信度衰减机制** — confidence 长期未验证自动衰减
4. **Gen-Ba 碎片协议** — 知识碎片标准格式，容许不完整，标记提取上下文
5. **多模态表征声明** — ObjectType 的感知通道描述

## References

- Polanyi, M. (1958). *Personal Knowledge: Towards a Post-Critical Philosophy*. University of Chicago Press.
- Polanyi, M. (1966). *The Tacit Dimension*. Doubleday.
- Nonaka, I. & Takeuchi, H. (1995). *The Knowledge-Creating Company*. Oxford University Press.
- Tong, S., Fan, D., LeCun, Y. et al. (2026). *Beyond Language Modeling: An Exploration of Multimodal Pretraining*. arXiv:2603.03276.
- Digital Knowledge Twin: Bridging Physical and Cyber Knowledge Spaces by Generative AI. IIAI Letters, 2025.
