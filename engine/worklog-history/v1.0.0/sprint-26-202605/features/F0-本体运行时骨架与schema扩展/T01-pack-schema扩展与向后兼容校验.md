# T01: semantic-pack schema 扩展与向后兼容校验

**优先级**: P0
**状态**: DONE
**依赖**: 无

## 目标

在现有 semantic-pack（`objects`/`synonyms`/`fewShots`/`guardrails`）基础上，定义四个新可选节 `links`/`metrics`/`signals`/`actions` 的 schema，并保证未声明这些节的旧 pack 零改动可加载。

## 技术设计

四节 schema（报花域示例）：

- **links**: `{ name, from, to, fromKey, toKey, cardinality, joinHint?, note? }`，描述对象间软外键关系。
- **metrics**: `{ name, object, expr, unit?, format?, caliber }`，集中定义派生指标口径。
- **signals**: `{ name, object, severity, when, advice, linkedActions? }`，阈值预警规则。
- **actions**: `{ name, object, intent, endpoint{service,draft,commit}, params[], approval, audit, guard }`，写回动作。

全部为**可选节**：解析器遇到缺省时返回空列表，不抛异常。

## 影响范围

- `dts-copilot-ai`：semantic-pack 解析模型（POJO/record）新增四节字段。
- `src/main/resources/semantic-packs/flowerbiz.json`：本 Task 只定义 schema，不填业务内容（内容在 F1/F2/F3 各自 Task 填）。
- schema 校验：可在加载期做轻量校验（必填字段缺失时记 WARN 并跳过该条，不中断加载）。

## 验证

- [x] 旧 pack（procurement/project-fulfillment/field-operations 三个未加四节的）加载无报错。
- [x] flowerbiz.json 加上四节空数组后加载无报错。
- [x] 四节内有非法条目（缺必填）时记 WARN 并跳过，不影响其余加载。

## 完成标准

- [x] 四节 schema 文档化（字段、必填、含义）。
- [x] 向后兼容单测通过。

## 证据

- `it/evidence/20260530-local/pack-schema-and-ontology-load.md`
- `mvn -pl dts-copilot-ai -Dtest=SemanticPackOntologySchemaTest,OntologyServiceTest test`
