# T03: sync 漂移检测 + 不可达降级

**优先级**: P0
**状态**: DONE
**依赖**: T02

## 目标

让 pack 与治理层之间的不一致**显式可见**，且治理层不可达时 agent 不被阻断——保证收口后既不漂移又不脆弱。

## 技术设计

1. **漂移检测**：定时/构建期把 pack 生成区与治理层最新导出比对（按版本哈希 + 逐规则 diff），不一致则告警（日志 + 健康端点字段 `caliberSyncDrift`）。可作为 CI gate。
2. **不可达降级**：沿用现有 OpenMetadata glossary 拉取的降级范式——
   - 治理层导出可达：用最新生成区。
   - 不可达：用上次成功缓存的生成区（带 `stale` 标记），再不可用则回退静态 guardrails / `BizEnumDictionary`。
   - 降级状态透出到 agent 响应的数据质量提示，不静默。
3. 刷新节奏对齐既有"每小时刷新 + 内存缓存"约定。

## 影响范围

- `dts-copilot-ai`：sync 检测 + 缓存/降级逻辑、健康端点字段
- 关联现有 glossary 拉取/降级代码（复用范式）

## 验证

- [x] 人为制造 pack 与治理层不一致 → 漂移告警触发：`CaliberGuardrailSyncServiceTest.shouldDetectGeneratedPackDriftAgainstGovernanceExport`
- [x] 断开治理层 → agent 仍可用，降级状态透出且不静默：`CaliberGuardrailSyncServiceTest.shouldKeepLastSuccessfulGovernanceExportWhenProviderFails` / `shouldFallbackToStaticPackGuardrailsWhenProviderFailsBeforeAnyCache`
- [x] 健康组件透出 `caliberSyncDrift` / `stale` / `fallbackMode`：`CaliberGuardrailSyncHealthIndicatorTest`
- [x] 恢复后自动回到最新生成区：provider 成功 refresh 会替换 `lastSuccessfulExport` 并清除 stale/fallback

> 当前默认 provider 为本地 `governance/caliber-rules.v1.json` 导出，保证 build/health gate 可跑；live dts-platform governance export 接入时只需替换 `GovernanceCaliberExportProvider`。

## 完成标准

- [x] 漂移可检测、可门禁；降级链路演练通过且有证据（入 F5/IT）
