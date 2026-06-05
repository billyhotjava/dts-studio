# Sprint-31 F3-T02 live platform submission evidence

**日期**: 2026-06-05
**范围**: dts-copilot-ai 语义指标草稿写入 dts-platform 治理指标 DRAFT

## 前置修复

- dts-copilot-ai: `finance` 本地域提交到平台时映射为 `CatalogDomain.code=S10-FIN`。
- dts-copilot-ai: 平台非 2xx 错误返回时保留响应体，live 调试可见 `HTTP 500`/`HTTP 400` 细节。
- dts-platform: `GovIndicatorDefinition` 的 `dynamic_filter_config`、`dependency_indicators`、`dimension_fields`、`join_config` 增加 Hibernate JSON 绑定，避免 jsonb 列按 varchar 写入。

## TDD / 构建验证

```bash
mvn -q -pl dts-copilot-ai -Dtest=SemanticDraftGovernanceSubmissionServiceTest,PlatformSemanticDraftClientTest test
./mvnw -q -Dtest=GovIndicatorDefinitionJsonbMappingTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -q -DskipTests package
```

## Live 提交结果

```json
{
  "indicatorCode": "codex_sprint31_live_finance_1780635038",
  "create": {
    "draftId": "semantic-draft-2",
    "status": "LOCAL_STAGED",
    "governanceStatus": "NOT_SUBMITTED",
    "validationErrors": []
  },
  "submit": {
    "draftId": "semantic-draft-2",
    "targetType": "GOVERNANCE_INDICATOR",
    "targetPath": "/api/governance/indicators",
    "platformId": "1ffc1036-ea0d-4bc2-9f47-86f78bb174cc",
    "platformStatus": "DRAFT",
    "governanceStatus": "DRAFT_SUBMITTED",
    "submitted": true,
    "error": ""
  }
}
```

## 平台读回

平台治理指标接口读回:

```json
{
  "message": "OK",
  "total": 1,
  "items": [
    {
      "id": "1ffc1036-ea0d-4bc2-9f47-86f78bb174cc",
      "code": "codex_sprint31_live_finance_1780635038",
      "domain": "S10-FIN",
      "status": "DRAFT",
      "version": "draft"
    }
  ]
}
```

平台库确认:

```text
code                                    | name                        | domain  | status | version
----------------------------------------+-----------------------------+---------+--------+--------
codex_sprint31_live_finance_1780635038  | Codex Sprint31 财务语义草稿 | S10-FIN | DRAFT  | draft
```
