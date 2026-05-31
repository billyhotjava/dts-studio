# T01: Indicator 产物类型

**优先级**: P0
**状态**: READY
**依赖**: F1(契约字段:`IndicatorDto` 经 BFF 透出的形状)

## 目标

扩展 sprint-27 F4 的活产物模型,新增第四种产物类型 `type: 'indicator'`,使平台治理指标成为画布上的一等产物。产物 spec 需同时承载:

1. **指标元数据**(用于渲染选择 + 后续 F4 口径芯片/F6 溯源):`name` / 口径 `definition` / `version` / `expressionSql` / `dimensionFields` / `timeGrain`。
2. **取值序列**:复用现有 `ArtifactDataset`(`cols`/`rows`)装 dashboard/detail 返回的聚合值,以便直接喂给 `ChartRenderer`/`DataTable`。
3. **下钻能力标记**:`drilldownEnabled` 布尔位 + 可下钻维度列表,供 F5 的下钻交互判断是否展示入口(本期只声明,不实现交互)。

并对齐 `useArtifactStore.upsert`:同一指标重复取值(切换天数/下钻)时用**同一 artifact id** 原地刷新,而非堆积新产物。

## 技术设计

文件:`/opt/prod/prs/source/dts-copilot/dts-copilot-webapp/src/types/artifact.ts`

### 1. 扩展 `ArtifactType`

```ts
// 现状: export type ArtifactType = "chart" | "table" | "report";
export type ArtifactType = "chart" | "table" | "report" | "indicator";
```

### 2. 新增指标元数据形状,挂进 `ArtifactSpec`

新增独立 interface(对齐 F1 透出的 `IndicatorDto` 子集,字段名与平台一致):

```ts
export interface IndicatorTimePoint {
	period: string;      // 时间桶标签(对齐 timeGrain)
	value: number | null;
}

export type IndicatorValueMode = "dashboard" | "detail" | "drilldown";

export interface IndicatorArtifactMeta {
	indicatorId: string | number;
	code?: string;
	name: string;
	definition?: string;        // 权威口径文案 → 后续 F4 口径芯片 / F6 溯源
	expressionSql?: string;     // 口径表达式 → 溯源「expressionSql」
	version?: string;           // 口径版本 vN
	category?: string;
	timeGrain?: string;         // day/week/month… → 渲染选趋势粒度
	dateColumn?: string;
	dimensionFields?: string[]; // 可下钻维度
	dataLevel?: string;         // 密级(展示用)
	owner?: string;
	valueMode?: IndicatorValueMode; // 当前产物取的是总览/明细/下钻
	drilldownEnabled?: boolean;     // F5 下钻入口能力标记
	stale?: boolean;            // 取值来自降级缓存时为 true(配合 F5)
}
```

在 `ArtifactSpec` 增可选字段(不破坏现有 chart/table/report 形状):

```ts
export interface ArtifactSpec {
	// …现有字段不变…
	indicator?: IndicatorArtifactMeta;
}
```

> `display`/`settings`/`dataset`/`trace`/`sourceRefs` 全部复用现有字段:indicator 产物的取值放进 `dataset`,趋势图类型放进 `display`(line/area),口径填进 `trace.metricCaliber`(对齐 F4/F6)。新增的只有 `indicator` 元数据块——零新增契约形状,符合设计 §6「响应契约……零新增契约形状」。

### 3. 新增构造工厂 `indicatorArtifact(...)`

与现有 `artifactFromMessage` 平行,新增一个由「指标目录项 + 取值结果」构造产物的纯函数(供 T03 资产库召唤调用):

```ts
export interface IndicatorArtifactInput {
	meta: IndicatorArtifactMeta;
	dataset: ArtifactDataset;          // 取值端点返回的聚合表
	display?: VisualizationType;       // 不传则由 resolveIndicatorDisplay 推断
	sourceMessageId?: string;          // 资产库召唤时用合成 id(见下)
	id?: string;                       // 同指标稳定 id → upsert 原地刷新
	createdAt?: number;
	settings?: VisualizationSettings;
}

export function indicatorArtifact(input: IndicatorArtifactInput): Artifact {
	const display = input.display ?? resolveIndicatorDisplay(input.meta, input.dataset);
	const sourceMessageId = input.sourceMessageId ?? `indicator:${input.meta.indicatorId}`;
	return {
		type: "indicator",
		id: input.id ?? makeIndicatorArtifactId(input.meta.indicatorId),
		sourceMessageId,
		createdAt: input.createdAt ?? Date.now(),
		title: input.meta.name,
		spec: {
			display,
			dataset: input.dataset,
			indicator: input.meta,
			...(input.settings ? { settings: input.settings } : {}),
			...(input.meta.definition || input.meta.expressionSql || input.meta.version
				? { trace: { metricCaliber: {
					name: input.meta.name,
					formula: input.meta.expressionSql,
					version: input.meta.version,
				} } }
				: {}),
		},
	};
}
```

### 4. 稳定 id 与 upsert 对齐

新增 `makeIndicatorArtifactId(indicatorId)`:同一指标 → **同一确定性 id**(如 `artifact:indicator:<normalizedId>`),不带随机 uuid。这样 T03 在「切天数 / 切总览↔明细」重新取值时调 `indicatorArtifact({ id: makeIndicatorArtifactId(id), ... })`,`useArtifactStore.upsert` 命中 `prev.some(item => item.id === artifact.id)` 分支 → 原地替换并 `setCurrentId`,实现「同 id 原地刷新」。
- `useArtifactStore.ts` 现有 upsert 逻辑无需改(已支持 exists→map 替换);只需保证 id 稳定。

### 5. `resolveIndicatorDisplay(meta, dataset)`

渲染类型推断(T02 也会复用):
- `meta.timeGrain` 存在且 dataset 含时间列(`dateColumn` / 首列可解析为时间桶)→ `"line"`(趋势)。
- 否则 → `"table"`(明细/多维)。
- 单点标量(1 行 1 数值列)可选 `"scalar"`。

## 影响范围

- 改:`src/types/artifact.ts`(新增类型 + 工厂 + id 生成器 + display 推断)。
- 测:`src/types/artifact.test.ts`(已存在)新增 indicator 用例。
- 不改:`src/hooks/useArtifactStore.ts`(现有 upsert 已满足,仅依赖稳定 id);若测试发现需暴露,再最小调整。
- 下游消费者:T02(渲染)、T03(召唤)依赖本 Task 导出的类型与 `indicatorArtifact`。

## 验证

- 单测:`indicatorArtifact` 由 meta+dataset 构造出 `type:'indicator'`、`spec.indicator` 完整、`trace.metricCaliber` 由口径字段填充。
- 单测:`makeIndicatorArtifactId` 对同 `indicatorId` 幂等(两次调用相同);`resolveIndicatorDisplay` 在有/无 timeGrain 下分别返回 line/table。
- 单测:连续两次 `indicatorArtifact` 同指标 → 同 id;喂进 `useArtifactStore.upsert` 后 `artifacts.length === 1`(原地刷新,不堆积)。
- 类型检查:`tsc --noEmit` 通过,现有 chart/table/report 产物构造不回归。

## 完成标准

- [ ] `ArtifactType` 含 `'indicator'`;`ArtifactSpec.indicator?: IndicatorArtifactMeta` 已定义且不破坏现有字段。
- [ ] `IndicatorArtifactMeta` 含 name/definition/version/expressionSql/dimensionFields/timeGrain/drilldownEnabled/stale。
- [ ] 导出 `indicatorArtifact`、`makeIndicatorArtifactId`、`resolveIndicatorDisplay`。
- [ ] 同指标重复构造 → 同 id,`useArtifactStore.upsert` 原地刷新(测试证明 `artifacts.length === 1`)。
- [ ] 新增单测全绿,`tsc --noEmit` 通过,现有 artifact 用例不回归。
