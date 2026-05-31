# T04: 前端指标 API 客户端(接 F1 BFF)

**优先级**: P0
**状态**: READY
**依赖**: F1(后端 BFF 端点:指标目录 + 取值 dashboard/detail/drilldown)

## 目标

在前端 API 层新增「平台指标」模块,对齐现有 `analyticsApi` 调 dts-copilot-ai BFF 的风格(`fetchJson`/`requestJson` + `/api/analytics/...` 路径 + 信封解包),提供:

1. **指标目录列表**:拉 F1 同步缓存的已发布指标目录(供 T03 资产库浏览)。
2. **指标取值**:`dashboard`(总览)/`detail`(单指标趋势)/`drilldown`(按维度下钻)三种取值。
3. **降级**:平台不可达 / 超时 / BFF 返回降级标记时,客户端返回**显式降级态**(空目录 + `degraded:true` + 文案),让 T03/T02 的 UI 友好提示而非抛未捕获异常。对齐设计 §7「显式降级,不静默」与 §6「有则用无则降级」。

> BFF 端点由 F1 提供;前端只对齐其路径与契约。路径采用与现有平台代理一致的 `/api/analytics/platform/indicators*` 前缀(参照现网 `listPlatformMetrics → /api/analytics/platform/metrics`)。最终路径以 F1 实现为准,本 Task 在客户端集中一处常量,便于对齐。

## 技术设计

新增文件:`/opt/prod/prs/source/dts-copilot/dts-copilot-webapp/src/api/modules/indicator.ts`

### 1. 类型(放 `src/api/types.ts`,与 `PlatformMetric` 同区)

```ts
export type PlatformIndicatorListItem = {
	id: string | number;
	code?: string;
	name: string;
	category?: string;
	definition?: string;
	expressionSql?: string;
	status?: string;        // 仅「已发布」会被 F1 同步出来
	version?: string;
	tags?: string[];
	dimensionFields?: string[];
	dateColumn?: string;
	timeGrain?: string;
	owner?: string;
	dataLevel?: string;
};

export type PlatformIndicatorCatalogResponse = {
	items: PlatformIndicatorListItem[];
	syncedAt?: string;
	degraded?: boolean;     // F1 用缓存兜底/平台不可达时为 true
	degradedReason?: string;
};

export type PlatformIndicatorValueResponse = {
	indicatorId: string | number;
	mode: "dashboard" | "detail" | "drilldown";
	// 直接对齐 ArtifactDataset,渲染零转换
	cols: { name: string; display_name?: string; base_type?: string }[];
	rows: unknown[][];
	timeGrain?: string;
	degraded?: boolean;
	degradedReason?: string;
};
```

### 2. 客户端模块(对齐 `database.ts` 风格)

```ts
import { fetchJson, HttpError } from "../httpClient.ts";
import type {
	PlatformIndicatorCatalogResponse,
	PlatformIndicatorValueResponse,
} from "../types.ts";

const BASE = "/api/analytics/platform/indicators";

const EMPTY_CATALOG: PlatformIndicatorCatalogResponse = {
	items: [],
	degraded: true,
	degradedReason: "平台指标服务暂不可达",
};

function degradedValue(
	mode: "dashboard" | "detail" | "drilldown",
	indicatorId: string | number,
	reason: string,
): PlatformIndicatorValueResponse {
	return { indicatorId, mode, cols: [], rows: [], degraded: true, degradedReason: reason };
}

export const indicatorApi = {
	// 目录:平台不可达 → 空目录 + degraded,不抛
	listPlatformIndicators: async (): Promise<PlatformIndicatorCatalogResponse> => {
		try {
			return await fetchJson<PlatformIndicatorCatalogResponse>(BASE);
		} catch (error) {
			return { ...EMPTY_CATALOG, degradedReason: describeIndicatorError(error) };
		}
	},

	getPlatformIndicatorDashboard: (days?: number) =>
		fetchIndicatorValue("dashboard", "_all", () =>
			fetchJson<PlatformIndicatorValueResponse>(
				`${BASE}/dashboard${days ? `?days=${encodeURIComponent(String(days))}` : ""}`,
			)),

	getPlatformIndicatorDetail: (id: string | number, days?: number) =>
		fetchIndicatorValue("detail", id, () =>
			fetchJson<PlatformIndicatorValueResponse>(
				`${BASE}/${encodeURIComponent(String(id))}/detail${days ? `?days=${encodeURIComponent(String(days))}` : ""}`,
			)),

	getPlatformIndicatorDrilldown: (
		id: string | number,
		dimension: string,
		period?: string,
	) =>
		fetchIndicatorValue("drilldown", id, () => {
			const qs = new URLSearchParams({ dimension });
			if (period) qs.set("period", period);
			return fetchJson<PlatformIndicatorValueResponse>(
				`${BASE}/${encodeURIComponent(String(id))}/drilldown?${qs.toString()}`,
			);
		}),
};

async function fetchIndicatorValue(
	mode: "dashboard" | "detail" | "drilldown",
	id: string | number,
	run: () => Promise<PlatformIndicatorValueResponse>,
): Promise<PlatformIndicatorValueResponse> {
	try {
		return await run();
	} catch (error) {
		return degradedValue(mode, id, describeIndicatorError(error));
	}
}

function describeIndicatorError(error: unknown): string {
	if (error instanceof HttpError) {
		return error.status >= 500
			? "平台指标服务暂不可达"
			: "平台指标取值失败";
	}
	return "平台指标服务暂不可达";
}
```

### 3. 挂进 `analyticsApi`

文件:`src/api/analyticsApi.ts`
- `import { indicatorApi } from "./modules/indicator.ts";`
- 加入 `export const analyticsApi = { ...indicatorApi, ... }`。
- 在顶部 `export type { ... } from "./types.ts"` 补 `PlatformIndicatorListItem` / `PlatformIndicatorCatalogResponse` / `PlatformIndicatorValueResponse`。

### 4. 降级语义约定(供 T02/T03)

- 目录降级:`degraded:true` + 空 `items` → T03 在「平台指标」tab 显示「平台指标服务暂不可达,稍后重试」空态,而非崩溃/白屏。
- 取值降级:`degraded:true` + 空 `cols/rows` → T03 不召唤产物 / T02 在画布显示降级 `CanvasState`;并为 F4「一键退回现生成 SQL」留钩子(本期至少不阻断、不静默)。
- 客户端**绝不 rethrow** 给 UI;所有跨服务失败收敛成 `degraded` 字段。

## 影响范围

- 新增:`src/api/modules/indicator.ts`。
- 改:`src/api/types.ts`(新增 3 个类型)、`src/api/analyticsApi.ts`(组合 + re-export)。
- 复用不改:`httpClient.ts`(`fetchJson`/`HttpError`)。
- 下游消费者:T03(资产库列目录 + 召唤取值)。

## 验证

- 单测(mock `fetchJson`):
  - `listPlatformIndicators` 正常 → 返回 items;`fetchJson` reject(网络/500)→ 返回 `{ items:[], degraded:true }`,**不抛**。
  - `getPlatformIndicatorDashboard/Detail/Drilldown` 正常 → 透传 cols/rows;失败 → 返回 `degraded:true` 的空值响应,`mode` 正确。
  - `describeIndicatorError`:`HttpError` 500 → 「暂不可达」,4xx → 「取值失败」。
  - drilldown 的 query 拼接含 `dimension`(+ 可选 `period`)。
- 类型检查:`analyticsApi.listPlatformIndicators` 等方法在 `analyticsApi` 上可见且类型正确。

## 完成标准

- [ ] `analyticsApi` 暴露 `listPlatformIndicators` / `getPlatformIndicatorDashboard` / `getPlatformIndicatorDetail` / `getPlatformIndicatorDrilldown`。
- [ ] 取值响应形状直接对齐 `ArtifactDataset`(cols/rows),T01/T02 渲染零转换。
- [ ] 任一调用在平台不可达/超时/错误时返回 `degraded:true` 的显式降级态,**不向 UI 抛未捕获异常**。
- [ ] 路径前缀集中常量化(`BASE`),便于与 F1 实际端点对齐。
- [ ] 新增单测全绿;`tsc --noEmit` 通过。
