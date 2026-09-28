# T01: 入湖与 dbt 新鲜度探针

**优先级**: P0
**状态**: DONE
**依赖**: F1-T01

## 目标

查询前后能知道目标 ADS/DWS 的上游 ODS 是否已入湖、dbt 是否构建成功、结果是否过期。

## 技术设计

- 通过 dts-platform `/api/etl/dbt/models/{model}/diagnostics` 读取最近任务状态。
- 目标 relation 反查 dbt model selector、上游 ODS、最后构建时间。
- 生成 `data.freshness`：`FRESH`、`STALE`、`MISSING`、`UNKNOWN`。
- 当前已完成请求上下文 `freshness` snapshot 的契约接入：`MISSING` 硬降为 `UNTRUSTED`，`STALE` 会把已对账 ADS/DWS 结果上限压到 `MEDIUM`。
- 已接入 live dts-platform/dbt diagnostics 客户端：无调用方 snapshot 时，聊天执行服务会按规划目标 ADS/DWS 自动补充 freshness。

## 影响范围

- dts-platform integration client。
- evidence assembler。
- health/metadata tests。

## 验证

- [x] snapshot 标记已构建 ADS 时返回 `FRESH` 并支持 `HIGH`。
- [x] snapshot 标记未入湖 ODS/未构建模型时返回 `MISSING` 并降为 `UNTRUSTED`。
- [x] snapshot 标记构建过期时返回 `STALE` 并将已对账 ADS/DWS 结果上限压到 `MEDIUM`。
- [x] live dts-platform/ingestion/dbt 元数据探针可直接给出以上状态。

## 完成标准

- [x] freshness 状态进入 evidence 并参与评分。
- [x] freshness 状态来自 live dts-platform/dbt 元数据，而非仅来自请求 snapshot。
