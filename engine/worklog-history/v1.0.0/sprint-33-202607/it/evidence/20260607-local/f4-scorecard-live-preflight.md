# F4 Scorecard Live Preflight

**时间**: 2026-06-07 20:10 Asia/Shanghai
**范围**: `dts-copilot-ai` runtime scorecard health + manual publish preflight
**状态**: IN_PROGRESS

## 运行前置

- 已执行 `mvn -pl dts-copilot-ai -Dtest=PlatformDbtFreshnessClientTest test`，新增 Spring bean wiring 回归覆盖 `PlatformDbtFreshnessClient` 作为 `DbtFreshnessResolver` 注册。
- 已执行 `mvn -pl dts-copilot-ai -DskipTests package`，重新生成 `dts-copilot-ai/target/dts-copilot-ai-1.0.0-SNAPSHOT.jar`。
- 已执行 `docker compose up -d --build copilot-ai`，镜像重新复制 82MB jar 并重建 `dts-copilot-ai`。
- 容器日志确认运行态加载 canonical registry：
  - `FinanceAuthorityRegistry`
  - `FinanceApplicationMysqlAuthorityRegistry`
- 运行态环境确认 `COPILOT_FINANCE_RECONCILIATION_LOCAL_EVIDENCE_ENABLED=true`；权威 L2 base URL 尚未配置。
- `docker compose ps copilot-ai` 显示 `dts-copilot-ai` 为 `healthy`。

## 命令

```bash
COPILOT_ADMIN_SECRET="<from dts-copilot-ai container env>" \
RUN_LIVE_F4_SCORECARD_PUBLISH=true \
bash worklog/v1.0.0/sprint-33-202607/it/test_f4_scorecard_live_preflight.sh
```

说明：脚本用 `COPILOT_ADMIN_SECRET` 创建临时 API key，未打印原始 key，并在退出时撤销临时 key。

## 输出

```text
health_url=http://localhost:50091/actuator/health
health_http_status=200
health_curl_exit=0
diagnostic.finance_reconciliation_health=present
diagnostic.finance_reconciliation_status=UP
diagnostic.temp_api_key_created=true
publish_url=http://localhost:50091/api/ai/finance/reconciliation-scorecards/publish-scheduled
publish_http_status=200
publish_curl_exit=0
diagnostic.publish_status=PENDING_LIVE_EVIDENCE
diagnostic.published_count=0
diagnostic.skipped_reason_code=PENDING_LIVE_EVIDENCE
diagnostic.failure_message=Finance reconciliation scorecard pending live evidence: scorecardId=sprint33-finance-daily-scorecard, category=f1-detail, checkId=detail-harness-live-evidence
status=PENDING_LIVE_EVIDENCE
reason=scorecard_publish_pending_live_evidence
next_action=enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes
```

## 结论

- 已修复运行态 stale jar 导致的 `publish-scheduled` 404：当前 endpoint 返回 200。
- 已修复运行态 `NO_EVIDENCE_PROVIDER_REGISTERED`：当前已注册 honest local readiness provider，但 publisher 检测到 F1 live 权威基准证据仍是 `PENDING_LIVE_EVIDENCE`，不会写入 scorecard snapshot。
- 当前不能标记 F4 live scorecard 完成：scheduled publisher 返回 `PENDING_LIVE_EVIDENCE`、`published_count=0`、`skipped_reason_code=PENDING_LIVE_EVIDENCE`，说明 F1/F2/F4 live required lanes 仍未满足，需要补真实 L2/adminapi 权威基准入口、F2 live tie-out 和 F4 差分网格真实行源。
