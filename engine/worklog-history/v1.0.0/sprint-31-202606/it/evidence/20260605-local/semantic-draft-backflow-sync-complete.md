# Sprint-31 F3-T03 semantic draft backflow sync complete evidence

**时间**: 2026-06-05
**环境**: local docker compose (`dts-platform`, `dts-copilot-ai`)
**结论**: PASS。已跑通一例 `草稿 -> 治理维护者发布 -> 平台 PUBLISHED -> Copilot sync 回流 -> Agent 使用已发布指标`。

## 范围与前置

- 平台指标: `codex_sprint31_live_finance_1780635038`
- 平台指标 id: `1ffc1036-ea0d-4bc2-9f47-86f78bb174cc`
- 指标名称: `Codex Sprint31 财务语义草稿`
- 回流机构上下文: `DTS_PLATFORM_ACTIVE_DEPT=1502`
- 本地 fixture:
  - 平台目录: `catalog_dataset` -> `public.codex_sprint31_indicator_fixture`
  - 查询数据源: `biadmin.public.codex_sprint31_indicator_fixture`

## 权限边界

1. Copilot 服务身份仍不能调用平台发布接口，发布动作保留在治理维护者权限内。
2. 发布验证使用临时本地治理维护者 portal session 执行，完成后已撤销。
3. Copilot sync 使用临时 API key 执行，完成后已撤销。
4. 敏感 token、admin secret、API key 未写入证据。

## 平台发布

`publish-preview` 结果:

```json
{
  "message": "OK",
  "ready": true,
  "blockerCount": 0,
  "validation": "SUCCESS",
  "rowCount": 1
}
```

`publish` 结果:

```json
{
  "message": "OK",
  "status": "PUBLISHED",
  "code": "codex_sprint31_live_finance_1780635038",
  "version": "v1"
}
```

平台 DB 校验:

```text
code=codex_sprint31_live_finance_1780635038
status=PUBLISHED
version=v1
owner_dept=1502
domain=S10-FIN
data_level=DATA_INTERNAL
last_validation_status=SUCCESS
```

## 回流同步

首次无机构上下文同步只返回全局指标:

```json
{
  "syncStatus": "SUCCESS",
  "fetched": 1,
  "catalogEntryCount": 1
}
```

根因: 平台 `IndicatorService.list()` 在未传 `X-Active-Dept` 时只暴露全局/root 指标，`ownerDept=1502` 的已发布指标不会进入服务读取结果。

修复: `dts-copilot-ai` 新增 `DTS_PLATFORM_ACTIVE_DEPT` 配置，平台指标客户端在配置后发送 `X-Active-Dept`。TDD 回归:

```bash
mvn -q -pl dts-copilot-ai -Dtest=PlatformIndicatorClientTest,PlatformIndicatorCatalogResourceTest,IndicatorCatalogSyncServiceTest,IndicatorMatcherServiceTest test
```

Result: PASS。

重建并以 `DTS_PLATFORM_ACTIVE_DEPT=1502` 重启后，手动 sync:

```json
{
  "syncStatus": "SUCCESS",
  "fetched": 2,
  "added": 0,
  "updated": 0,
  "removed": 0,
  "catalogEntryCount": 2,
  "entryCount": 2,
  "lastStatus": "SUCCESS",
  "stale": false,
  "error": null
}
```

## Agent 使用

请求:

```text
POST /api/ai/agent/chat/send
message="查询 Codex Sprint31 财务语义草稿"
X-DTS-Dept=1502
```

结果:

```json
{
  "responseKind": "PUBLISHED_INDICATOR",
  "reportCode": "codex_sprint31_live_finance_1780635038",
  "targetView": "indicator:codex_sprint31_live_finance_1780635038",
  "dataSurface": "L3_PUBLISHED_INDICATOR",
  "qualityLevel": "HIGH",
  "sourceRefs": "platform-indicator:codex_sprint31_live_finance_1780635038",
  "metricCaliber": {
    "name": "Codex Sprint31 财务语义草稿",
    "formula": "select sum(amount) as metric_value, max(level) as level from public.codex_sprint31_indicator_fixture",
    "domain": "S10-FIN",
    "version": "v1",
    "ontologyRef": "1ffc1036-ea0d-4bc2-9f47-86f78bb174cc"
  }
}
```

## 判定

- 未审 DRAFT 不进入回流目录: PASS（见 `semantic-draft-backflow-sync-partial.md`）
- 治理维护者发布后进入平台 SoT: PASS
- Copilot 按机构上下文同步 PUBLISHED 指标: PASS
- Agent 命中并使用新增平台指标: PASS
