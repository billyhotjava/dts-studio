# F4: 新场景端到端验证

**优先级**: P1
**状态**: DONE

## 目标

用场景接入套件（F3）实际接入 1 个空白域，端到端跑通 domain→dbt→pack→路由→联邦，并验证多场景共存时口径不串、命名空间不撞、按 domain 隔离——证明范式可复制。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 一个空白域端到端接入 | P1 | DONE | F3 |
| T02 | 多场景共存隔离验证 | P1 | DONE | T01 |

## Task 明细

### T01 一个空白域端到端接入
- **目标**：择一空白域（库存 / 督导 / 薪资，按业务价值与口径敏感度选，建议库存）用套件跑通全链路。
- **设计**：照 F3 手册：注册 CatalogDomain → 建 `<scene>_*` dbt 五层 → ODS 血缘补全（沿用 sprint-30 F2 范式）→ 场景 pack（guardrails 走 Sprint-31 sync 生成）→ Trino catalog → 路由纳入 F1 阶梯。该域口径铁律（如库存加权平均成本、good_price_id 关联）按 Sprint-31 规则化收口。
- **影响**：新增该域 dbt 模型 + pack + 治理层域定义 + 路由注册。
- **验证**：库存源表发现与骨架实例化可重跑 `../../it/test_f4_inventory_source_discovery.sh`；Agent runtime 已加载 `warehouse` 语义包，并新增 `TPL-53/TPL-54` 库存弱路径模板；库存 dbt ADS 模型定义可重跑 `../../it/test_f4_inventory_dbt_ads_models.sh`；运行态已导入 `ods_ptr_mysql_s_stock_info`，dbt build 成功，ADS 对账通过，证据见 `../../it/evidence/20260603-local/f4-inventory-dbt-runtime-build.md` 与 `../../it/evidence/20260603-local/f4-inventory-ads-reconciliation.md`。

### T02 多场景共存隔离验证
- **目标**：确认报花/财务/采购 + 新域并存时互不污染。
- **设计**：验证 ①口径不串（新域 SUM 不混入既有域结算链）②`<scene>_*` namespace 不撞 ③路由按 domain 正确分流 ④Trino catalog 隔离。明确"软隔离（domain 分组）vs 硬隔离（deploy-per-scenario）"边界并记录。
- **影响**：跨域回归用例。
- **验证**：跨域混合问题集已固化为 `../../it/test_f4_multi_scenario_isolation.sh`，采购/报花/库存 4 条问题均返回非空结果，口径与归属正确，无命名空间冲突。

## 完成标准

- [x] 1 个空白域用套件端到端跑通，口径与对账达标
- [x] 多场景共存隔离验证通过，软/硬隔离边界记录在案
