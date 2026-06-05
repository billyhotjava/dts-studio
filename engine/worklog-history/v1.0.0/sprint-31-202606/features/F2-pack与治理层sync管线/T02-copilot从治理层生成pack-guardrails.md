# T02: copilot 从治理层生成 pack guardrails

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

在 dts-copilot 侧建立 pack 生成器：消费治理层导出契约，产出 finance/procurement pack 的 guardrails 与口径相关字段，使手维护下线。

## 技术设计

- 新增生成器（构建期脚本或启动期服务），输入 = 治理层导出（T01），输出 = pack 的 `guardrails` 段 + 口径标注。
- pack 文件结构区分**生成区**与**手维护区**（fewShots 等 AI 可供性仍手维护/草稿）：生成区加显式标记（如 `"_generated": true, "_source": "governance@<version>"`），并加 lint 阻止手改生成区。
- 沿用现有 `SemanticPackService.PACK_FILES` 加载链，不改运行时消费方式，只改"guardrails 从哪来"。
- 兼容旧 pack：生成区缺失时回退到现有静态 guardrails（平滑迁移）。

## 影响范围

- `dts-copilot-ai`：pack 生成器 + `semantic-packs/finance.json`、`procurement.json` 的 guardrails 段改为生成产物
- 可能新增 `scripts/generate-semantic-pack.*` 与 pack lint 校验
- 关联 sprint-30 F3 guardrail 消费逻辑（消费方式不变）

## 实施记录

- 2026-06-05：`finance.json` / `procurement.json` 新增 `generatedGuardrails` 生成区，标注 `_generated=true` 与 `_source=governance/caliber-rules.v1.json`。
- 2026-06-05：`SemanticPackService` 解析时优先使用生成区，并按 `CAL-*` rule id 对旧静态护栏去重；旧 pack 缺失生成区时仍回退 `guardrails` 数组。
- 2026-06-05：新增 `SemanticPackGovernanceGuardrailTest`，校验生成区与 `CaliberRuleRegistry` 完全一致，同时保留采购/财务的手维护 operational guardrails。

## 验证

- [x] 生成的 finance/procurement guardrails 与治理层规则逐条对应
- [x] 手改生成区被测试阻断（生成区必须与 registry 完全一致）
- [x] 现有 `SemanticPackCaliberGuardrailTest` 仍绿（消费侧不变）

## 完成标准

- [x] guardrails 由生成器产出，pack 不再独立漂移；旧 pack 平滑兼容
