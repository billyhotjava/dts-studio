# F2: pack ⇄ 治理层 sync 管线

**优先级**: P0
**状态**: READY

## 目标

把 dts-copilot pack 的口径 guardrails / 指标定义从"手维护"改为"从治理层 SoT 生成"，并建立漂移检测与不可达降级，使 pack 永远是治理层的投影、不再独立漂移。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 治理层口径/指标定义导出契约 | P0 | READY | F1-T02,T03 |
| T02 | copilot 从治理层生成 pack guardrails | P0 | READY | T01 |
| T03 | sync 漂移检测 + 不可达降级 | P0 | READY | T02 |

## 完成标准

- [ ] 治理层有稳定导出契约（API 或制品），输出口径规则 + 指标定义
- [ ] finance/procurement pack 的 guardrails 由生成器产出，手维护字段标注"勿手改"
- [ ] sync 漂移检测可跑：生成结果与当前 pack 不一致时报警
- [ ] 治理层不可达时按既有 glossary→BizEnumDictionary 范式降级，不阻断 agent
