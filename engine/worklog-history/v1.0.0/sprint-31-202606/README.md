# Sprint-31: 语义口径单一事实源收口（治理债先还）

**时间**: 2026-06
**前缀**: SG (Semantic Governance)
**状态**: IN_PROGRESS
**目标**: 消除 pack / dbt mart / OpenMetadata glossary 三源口径漂移，确立治理层为口径**单一事实源（SoT）**，建立 pack⇄治理 sync 与本体/指标草稿晋升闭环，把 9 条口径铁律编码为机器可检规则。

## 北极星目标（本 sprint 承接的总目标）

> 让 agent 在多业务场景下可靠地 NL2SQL 拿到**口径正确**的业务数据，且新场景接入可复制。

本 sprint 是这条总目标的**地基**：在扩数据/铺场景之前，先保证"口径只有一个真相"。Sprint-32 负责路由阶梯与多场景接入套件，依赖本 sprint 的语义收口成果。

## 背景

三轮架构勘探（基于真实代码）得出的必须先处理的结构性风险：

1. **三源口径漂移（最大风险）**：同一口径规则现存三处可独立改动的定义——
   - dts-copilot `semantic-packs/*.json` 的 guardrails / fewShots（agent 朝向）
   - dts-stack `xycyl_ads_*` dbt 模型物化的口径（数据朝向）
   - OpenMetadata / dts-platform `modeling` 模块的 glossary / 数据标准（治理朝向）
   三者手维护必然漂移，而漂移点恰是 9 条口径铁律（biz_type 三套枚举、两条结算链、月对账三级金额…），即 agent 静默算错钱的地方。
2. **本体当前只活在 copilot pack 里**（sprint-26）：是高速落地的对的选择，但缺与治理层的 sync/promote 管线，长期会成为"第三套事实源"。
3. **dts-platform 无 ontology 模块**：语义能力实际在 `modeling`（DataStandard + GlossaryTerm + ModelingPlan + SemanticModelingService），且 `modeling` 耦合 catalog/dbt 较深——不适合做可移植语义层，适合做**每部署的治理引擎**。

## 设计依据

- 三轮架构讨论结论：统一语义治理（一个口径 SoT）+ 统一访问平面（Trino），不强求统一物理存储。
- 本体二分：**权威定义在治理层、agent 投影 + 草稿面在 copilot pack**。
- 复用 sprint-26 "草稿端点 + 人审晋升、绝不直写"范式（这次复制到"写回语义定义"）。
- 复用 sprint-30 F3 口径 guardrail 回归网与 dts-stack dbt 范式。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 说明 |
|----|---------|---------|--------|------|------|
| F1 | 口径事实源对账与定源 | 4 | P0 | IN_PROGRESS | 三源差异盘点 + 定 SoT + 9 铁律机器规则化 |
| F2 | pack ⇄ 治理层 sync 管线 | 3 | P0 | DONE | guardrails 从治理层生成，替代手维护；漂移/降级 gate 已落地 |
| F3 | 本体/指标草稿晋升闭环 | 3 | P1 | DONE | copilot 起草 → 治理 draft → 审通过回流 |
| F4 | 跨源口径回归网 | 2 | P1 | DONE | T01 三源一致性 local fixture 门禁 + T02 铁律违规 SQL 拦截回归已完成 |
| F5 | 范式固化与 IT 证据 | 2 | P2 | READY | 收口手册 + 可复现证据包 |

## 依赖顺序

```
F1(对账定源) ──┬──> F2(sync 下发) ──┐
               └──> F3(草稿晋升) ───┴──> F4(回归网) ──> F5(范式+IT)
```

## 本 sprint 不做

- 不做 5 层路由阶梯与 telemetry（Sprint-32 F1）。
- 不铺新业务场景 / 不做场景接入套件（Sprint-32 F3/F4）。
- 不抽取 dts-platform `modeling` 为独立微服务（架构判断：耦合度证明 ROI 差）。
- 不一次性把全部域口径都收口，先收 finance + procurement 两个已建语义包的域，跑通收口范式。

## 完成标准

- [ ] 产出三源口径差异台账，逐条标注"以哪个为准"，并形成收口 ADR
- [ ] 治理层确立为口径 SoT，9 条口径铁律全部编码为机器可检规则（含可跑校验）
- [x] dts-copilot 的 finance/procurement pack guardrails 改为**从治理层生成**，手维护下线
- [x] sync 漂移检测可跑，治理层不可达时按既有 stale cache/static pack 范式降级
- [x] copilot 具备"语义草稿"端点，草稿落治理层 draft、审通过后回流 pack，全程不直改 SoT（2026-06-05 live：F3-T03）
- [x] 跨源口径一致性回归绿（pack 生成结果 == dbt 口径 == glossary；local fixture 门禁，live manifest/glossary 接入待后续）
- [ ] `it/README.md` 有真实可重跑证据（差异盘点、sync 生成、回归绿、降级演练）

## 相邻 sprint 关系

- 输入：本轮三轮架构讨论结论、sprint-26 本体化、sprint-30 口径 guardrail 与财务垂直切片。
- 输出：口径 SoT + sync 管线 + 草稿晋升闭环 → Sprint-32 路由阶梯每层"从治理层解析口径"的前置；多场景接入套件复用本 sprint 的收口范式。
