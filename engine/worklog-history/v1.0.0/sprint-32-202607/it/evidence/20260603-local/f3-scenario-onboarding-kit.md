# F3 场景接入套件证据

**日期**: 2026-06-03
**环境**: local `/opt/prod/prs/source/dts-copilot`
**范围**: Sprint-32 F3/T01-T03

## 目标

验证场景接入套件已经从纸面 checklist 变成可执行骨架：

- 六要素模板齐全。
- 报花/财务两个已接入域有样例映射。
- 脚手架可生成库存域骨架。
- 生成物可被后续 F4 端到端验证填充。

## 重跑命令

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f3_scenario_onboarding_kit.sh
```

结果：

```text
[F3] scenario onboarding kit scaffold verified
```

## 覆盖点

| 覆盖点 | 结果 |
|--------|------|
| `assets/scenario-onboarding-kit/README.md` 存在 | PASS |
| `examples/flowerbiz.md` / `examples/finance.md` 存在 | PASS |
| 六要素模板存在 | PASS |
| `scripts/scaffold-scenario-kit.mjs --dry-run` 可用 | PASS |
| 临时库存域骨架可生成 | PASS |
| `catalog-domain.json` / semantic pack JSON 可解析 | PASS |
| dbt `ref('inventory_stg_placeholder')` 保留 | PASS |
| 路由模板包含 `mysql.rs_cloud_flower` 联邦源约定 | PASS |

## 结论

F3 已具备可执行接入套件。F4 可以直接基于库存域执行：

```bash
node scripts/scaffold-scenario-kit.mjs --scene-code inventory --domain-name "库存" --owner warehouse-team
```

然后把占位模型替换为 `s_stock_info`、出入库、物品价格主数据等真实库存链路。
