# Sprint-31 F3-T03 semantic draft backflow sync partial evidence

**时间**: 2026-06-05
**环境**: local docker compose (`dts-platform`, `dts-copilot-ai`)
**结论**: PARTIAL PASS。已验证未审草稿不会进入 Copilot 回流目录，且新增手动 sync 端点可触发平台 PUBLISHED 指标回流；完整“治理维护者发布后 agent 使用新指标”仍待治理维护者会话执行。
**后续**: 完整闭环已在同日补齐，见 `semantic-draft-backflow-sync-complete.md`。

## 已验证链路

1. `dts-copilot-ai` 重建并重启健康。
2. 新增端点可访问，`POST /sync` 需要有效 API key 与 `X-Admin-Secret`：
   - `GET /api/copilot/platform-indicators/status`
   - `POST /api/copilot/platform-indicators/sync`
3. 平台 DRAFT 草稿仍停留在治理草稿态：

```json
{
  "message": "OK",
  "total": 1,
  "content": [
    {
      "id": "1ffc1036-ea0d-4bc2-9f47-86f78bb174cc",
      "code": "codex_sprint31_live_finance_1780635038",
      "name": "Codex Sprint31 财务语义草稿",
      "domain": "S10-FIN",
      "status": "DRAFT",
      "version": "draft"
    }
  ]
}
```

4. Copilot 服务身份尝试发布该草稿返回 `HTTP 401`，符合“Copilot 不代审、不直发”的权限边界。
5. 平台 PUBLISHED 指标当前只有 1 条：

```json
{
  "message": "OK",
  "total": 1,
  "content": [
    {
      "id": "29000000-0000-4000-8000-000000000029",
      "code": "codex_sprint29_live_metric",
      "name": "Sprint29 验证指标",
      "domain": "ops",
      "status": "PUBLISHED",
      "version": "v1"
    }
  ]
}
```

6. Copilot 手动 sync 只回流 PUBLISHED 目录，未纳入上述 DRAFT 草稿（最终验证时间：2026-06-05T05:13:59Z）：

```json
{
  "status": "SUCCESS",
  "fetched": 1,
  "added": 0,
  "updated": 0,
  "removed": 0,
  "caliberChangedCodes": [],
  "error": null,
  "catalogEntryCount": 1,
  "entryCount": 1,
  "stale": false,
  "lastStatus": "SUCCESS"
}
```

## 待补

- 使用治理维护者用户会话执行平台 `publish-preview`/`publish`。
- 发布成功后再次调用 `POST /api/copilot/platform-indicators/sync`，确认新增正式指标进入回流目录，并在 agent 指标匹配/回答中体现。
