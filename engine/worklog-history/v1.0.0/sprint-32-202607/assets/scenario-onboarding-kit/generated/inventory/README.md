# 库存场景接入骨架

**sceneCode**: `inventory`
**owner**: `warehouse-team`
**源入口**: `mysql.rs_cloud_flower`
**数仓入口**: `postgres.public`

## 生成物

| 路径 | 说明 |
|------|------|
| `catalog-domain.json` | CatalogDomain 注册草稿 |
| `dbt/models/` | `<scene>_*` 首版库存 STG/DWD/DWS/ADS 模型 |
| `semantic-packs/inventory.json` | Agent 语义包模板 |
| `trino-catalog/inventory.properties` | Trino catalog 配置片段 |
| `glossary/inventory-glossary.yml` | 业务词表模板 |
| `routing/inventory-route-map.md` | 五层路由接线模板 |

## 填充顺序

1. 补齐 `catalog-domain.json` 中的源表、业务对象、口径陷阱。
2. 将 `dbt/models/**` 合入正式 dbt 项目，并按运行环境把 `inventory_*_relation` var 指向 ODS 或联邦源。
3. 在 `semantic-packs/inventory.json` 中补对象、fewShot 和 guardrail。
4. 确认 `trino-catalog/inventory.properties` 指向只读或读副本连接。
5. 用 `routing/inventory-route-map.md` 明确路由层级和弱路径升级规则。
6. 在 sprint `it/` 中增加对账和跨域隔离验证脚本。

## 完成门槛

- 至少一个 ADS 或已认证业务对象可回答典型问题。
- 所有 SQL 使用 `catalog.schema.table` 三段式。
- 高危口径问题进入 semantic pack guardrail。
- 与业务页面或源库 SQL 有可重跑对账证据。
