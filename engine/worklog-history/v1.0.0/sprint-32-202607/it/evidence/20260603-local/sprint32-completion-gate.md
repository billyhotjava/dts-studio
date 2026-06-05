# Sprint-32 completion gate evidence

**日期**: 2026-06-03
**范围**: Sprint-32 全量完成门禁

## 目的

Sprint-32 不能只靠单项测试通过就标记 DONE。完成标准横跨：

- F1 路由责任链、routeTrace、telemetry。
- F2 Trino 只读/限流/Ranger/资源护栏。
- F3 场景接入套件。
- F4 库存域端到端 dbt build 与 ADS 对账。
- F5 IT 证据包。

因此新增 `it/test_sprint32_completion_gate.sh`，将 sprint DONE 的必要条件固化为可重跑门禁。

## 当前运行结果

命令：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_sprint32_completion_gate.sh
```

初始结果：

```text
[sprint32] completion gate FAILED (8 gap(s))
- sprint README still has non-DONE feature rows
- IT README still has TODO or PARTIAL_PASS evidence rows
- sprint README still has incomplete completion criteria
- rendered dts-trino MySQL user is privileged; remove high-privilege TRINO_MYSQL_* overrides and use TRINO_MYSQL_READONLY_*
- dts-stack .env still explicitly overrides TRINO_MYSQL_USER; use TRINO_MYSQL_READONLY_USER instead
- Trino Ranger/access-control policy is not wired for federated catalogs
- missing inventory runtime dbt build evidence
- missing inventory ADS reconciliation evidence
```

## 结论

该失败结果是预期的。它证明 Sprint-32 当前不能标记 DONE，剩余工作必须围绕：

1. F2: 切换运行态 Trino MySQL 只读账号，补 Ranger/access-control 配置与运行态验证。
2. F4: 将库存模型合入正式 dbt 发布链路，执行 runtime build，并补 ADS 对账证据。
3. F5: 所有 IT 项从 `TODO/PARTIAL_PASS` 转为真实 PASS 后，completion gate 才能通过。

## 本轮补充修复

completion gate 同时检查 `dts-stack/init.sh`，防止重新初始化时再次生成高权限 Trino MySQL 默认账号。本轮已修复该生成源头：

- `init.sh` 默认生成 `TRINO_MYSQL_READONLY_USER=trino_readonly`。
- `TRINO_MYSQL_USER` 默认继承 `TRINO_MYSQL_READONLY_USER`。
- `init.sh` 不再包含 `TRINO_MYSQL_USER:=root`。

当前失败项仍然包含运行态 `.env` 显式覆盖，因为现有部署文件仍需要切换到只读账号并重建 `dts-trino`。

## 本轮运行态推进

已完成：

- `TRINO_MYSQL_READONLY_*` 切到数据源 15 的受限业务账号。
- 重建并重启 `dts-trino`。
- Trino MySQL catalog 读通 `mysql.rs_cloud_flower.s_stock_info`，返回 `11661`。
- `system` catalog 与 MySQL 写表均被 access-control 拒绝。
- 库存 ODS 导入 `11661` 行。
- 库存 dbt runtime build 成功：6 个模型建表，`PASS=12 WARN=2 ERROR=0`。
- 库存 ADS 对账通过：overview `11660` 行，low stock alert `9971` 行。

文档状态同步后重跑 completion gate：

```text
[sprint32] completion gate PASS
```

## 最终结论

Sprint-32 completion gate 当前通过。该门禁已确认：

- Sprint README 无非 DONE feature 行。
- IT README 无 TODO/PARTIAL_PASS 证据行。
- Sprint 完成标准无未勾选项。
- dts-stack Trino readonly/access-control/resource group 配置存在。
- `f4-inventory-dbt-runtime-build.md` 与 `f4-inventory-ads-reconciliation.md` 两份库存运行证据存在。

## 使用规则

后续每次准备把 Sprint-32 或 active goal 标记完成前，必须先运行：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_sprint32_completion_gate.sh
```

只有 exit code 为 `0` 时，才允许把 Sprint-32 状态改为 DONE。
