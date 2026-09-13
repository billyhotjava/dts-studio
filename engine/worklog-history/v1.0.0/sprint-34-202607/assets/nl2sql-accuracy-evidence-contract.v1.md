# NL2SQL Accuracy Evidence Contract v1

## 目标

`accuracyEvidence` 是每次 NL2SQL 回答随结果返回的准确性证据包。它不替代 SQL 结果，而是说明这个结果为什么可信、缺哪些证明、是否允许业务采信。

## 契约草案

```json
{
  "grade": "HIGH",
  "score": 0.92,
  "reasons": ["命中受控模板 TPL-57", "目标表为 ADS", "dbt 测试通过", "凭证数与 STG 对账一致"],
  "warnings": [],
  "intent": {
    "question": "2026年凭证的数据统计下",
    "domain": "finance",
    "intentType": "YEARLY_VOUCHER_SUMMARY",
    "params": {"year": "2026"},
    "matchedBy": "TEMPLATE"
  },
  "route": {
    "routeTier": "T2_ADS_TEMPLATE",
    "templateCode": "TPL-57",
    "metricCode": null,
    "dataSurface": "ADS",
    "targetRelations": ["public.xycyl_ads_finance_voucher_monthly"]
  },
  "sql": {
    "sqlHash": "sha256:...",
    "safeSql": "SELECT ...",
    "staticChecks": ["READ_ONLY", "ALLOWED_RELATION", "PARAM_BOUND"]
  },
  "data": {
    "freshness": "FRESH",
    "lastIngestionTask": "ptr_mysql_flow",
    "lastDbtRun": "xycyl_ads_finance_voucher_monthly",
    "lineage": ["ODS:f_voucher", "STG", "DWD", "DWS", "ADS"]
  },
  "tieout": {
    "status": "PASS",
    "checks": [
      {
        "name": "voucher_count_stg_vs_ads",
        "expected": 663,
        "actual": 663,
        "diff": 0
      }
    ]
  }
}
```

## 评分规则

| 维度 | 权重 | HIGH 要求 |
|------|------|-----------|
| 意图解析 | 20 | 模板/指标/语义包命中，参数完整 |
| 数据层 | 20 | ADS/DWS 或治理指标；非 L0 profile |
| SQL 安全与口径 | 20 | 只读、白名单表、参数绑定、口径 guardrail 通过 |
| 数据新鲜度 | 20 | 入湖/dbt 构建成功，模型未过期 |
| 对账证明 | 20 | 权威基准/tie-out/不变量至少一种通过 |

`grade` 由硬规则优先决定，再参考 `score`：

- 有异常、未授权、非只读、禁用数据层：`UNTRUSTED`
- 无对账但 ADS/DWS 正常：最高 `MEDIUM`
- 弱路径联邦或自由 SQL：最高 `LOW`
- 全部 HIGH 要求满足：`HIGH`

## 非目标

- 不把 LLM 自评作为准确性证据。
- 不把“执行成功”单独作为准确性证据。
- 不展示明文 JDBC、token、password 或未脱敏 SQL。
