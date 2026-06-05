# 报花 / PRS 租摆域套件样例

| 六要素 | 当前落点 |
|--------|----------|
| CatalogDomain | `flowerbiz`，PRS 报花 / 绿植租摆业务域 |
| dbt namespace | `xycyl_stg_flower_*`、`xycyl_dwd_flowerbiz_*`、`xycyl_dws_flowerbiz_*`、`xycyl_ads_flowerbiz_*` |
| semantic pack | `dts-copilot-ai/src/main/resources/semantic-packs/flowerbiz.json` |
| Trino catalog | `postgres.public.*` for dbt warehouse；`mysql.rs_cloud_flower.*` for source drilldown |
| glossary | 口径已固化在 dbt ADS 字段、semantic pack guardrails 和 fixed report asset metadata |
| routing | Tier 2 优先命中 PRS ADS/资产；明细排查落 Tier 4/5 并进入 route telemetry |

## 已认证 ADS

- `public.xycyl_ads_flowerbiz_overview`
- `public.xycyl_ads_flowerbiz_lease_summary`
- `public.xycyl_ads_flowerbiz_sale_summary`
- `public.xycyl_ads_flowerbiz_pending`
- `public.xycyl_ads_flowerbiz_recovery_detail`
- `public.xycyl_ads_flowerbiz_audit_trail`

## 口径护栏

- 租赁、销售、坏账、额外费用不要混作一个收入口径。
- 客户维度存在关联缺失风险，客户级排行必须保留质量提示。
- recovery 覆盖度偏低，回收结论适合明细排查和趋势参考。
- 自动预览走 Trino 时必须使用 `postgres.public.*` 或 `mysql.rs_cloud_flower.*`。

## 对 F4 的启发

库存域应像报花域一样，先确定 ADS 默认查询面，再允许业务对象明细作为弱路径兜底。低库存预警不能长期停留在旧 fixed report 占位资产。
