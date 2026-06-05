# 场景接入套件

本套件把"接入一个新业务场景"固化为可复制骨架。它不是自动建模器，而是把每个场景都必须补齐的六个交付物一次性生成出来，避免新场景靠口头经验接入。

## 六要素

| 要素 | 生成物 | 作用 | 完成门槛 |
|------|--------|------|----------|
| 1. CatalogDomain | `catalog-domain.json` | 声明业务域、负责人、源系统、口径风险和路由层级 | 域名、owner、源表、核心口径陷阱齐全 |
| 2. dbt namespace | `dbt/models/{stg,dwd,dws,ads}` | 固化 `<scene>_*` 五层模型命名空间 | 至少 STG/DWD/DWS/ADS 各有真实模型 |
| 3. semantic pack | `semantic-packs/<scene>.json` | 给 Agent 提供对象、同义词、fewShot、guardrail | 高危问句有 guardrail 和 fewShot |
| 4. Trino catalog | `trino-catalog/<scene>.properties` | 声明联邦查询接入片段 | 只读账号、读副本或受限源、catalog/schema 明确 |
| 5. glossary | `glossary/<scene>-glossary.yml` | 口径词表和业务字段标准 | 与 dbt 字段、业务页面语言一致 |
| 6. routing | `routing/<scene>-route-map.md` | 纳入五层路由阶梯 | 明确默认优先级、弱路径和建 ADS 触发条件 |

## 脚手架

```bash
node scripts/scaffold-scenario-kit.mjs \
  --scene-code inventory \
  --domain-name "库存" \
  --owner "warehouse-owner"
```

默认输出：

```text
worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory/
```

可用参数：

- `--source-catalog` / `--source-schema`：默认 `mysql.rs_cloud_flower`。
- `--warehouse-catalog` / `--warehouse-schema`：默认 `postgres.public`。
- `--output-dir`：用于测试或临时生成。
- `--dry-run`：只打印将生成的文件。
- `--force`：覆盖已生成目录。

## 与 Sprint-30 checklist 的关系

Sprint-30 的 `blank-domain-onboarding-checklist.md` 是纸面流程，本套件把每一步落成文件：

| Sprint-30 步骤 | 本套件落点 |
|----------------|------------|
| 源表盘点、口径陷阱 | `catalog-domain.json` |
| ODS/STG/DWD/DWS/ADS | `dbt/models/**` |
| 语义包 | `semantic-packs/<scene>.json` |
| 回归测试、对账 | `README.md` 的验证区 + F4 IT 脚本 |
| Trino 联邦 | `trino-catalog/<scene>.properties` |
| 证据包 | sprint-32 `it/evidence/**` |

## 已填样例

- `examples/flowerbiz.md`：PRS 报花/租摆域，已接入 ADS + screen 资产。
- `examples/finance.md`：财务域，展示结算、开票、收款三类 ADS 的套件映射。

## 后续使用

F4 新场景验证建议选择库存域。生成骨架后，先补 `s_stock_info`、出入库、物品价格主数据相关模型，再把低库存预警从旧 fixed report 占位资产迁移到正式库存场景资产。
