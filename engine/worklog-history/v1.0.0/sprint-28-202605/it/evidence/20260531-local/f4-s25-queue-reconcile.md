# F4-T03 S25 Queue 回写证据

## 范围

复核 Sprint-25 当前真实状态,把过期“等入湖”阻塞从 queue 中解除,同时保留业务口径拍板未完成的事实。

## 改前

`worklog/v1.0.0/sprint-queue.md` 中 S25 状态:

| Feature | 改前状态 |
|---------|----------|
| F0-项目域P0数据画像与口径决策 | BLOCKED |
| F1-共享维度与项目域dbt建模 | BLOCKED |
| F2-项目域NL2SQL接入 | BLOCKED |
| F3-项目域回归与验收 | BLOCKED |

统计:`READY=0, IN_PROGRESS=0, DONE=0, BLOCKED=4`。

## 证据

入湖阻塞已解除:

- `project-ingestion-runtime.md`: task `46` / execution `83` 成功,11 张 Sprint-25 ODS 均已入数。
- `project-profile-after-ingestion.md`: 入湖后 source profile 可复核。

工程 baseline 已完成:

- `project-dbt-build-after-ingestion.md`: `dbt build --select tag:xycyl-project` 结果 `PASS=76 WARN=1 ERROR=0`。
- `project-nl2sql-dbt-routing.md`: project semantic pack、report catalog、business object catalog 与 8 个 query templates 指向 dbt marts。
- `project-golden-questions.md`: 15 条 Golden Questions,12 条 mart fast path,7 个目标表存在且有数据。
- `project-adminweb-reconcile.md`: ProjectSummary 对账 7/7 PASS,最大误差 `0.0000%`。

仍未完成:

- `F0-T03` 业务口径决策仍 `BLOCKED`: `rent/cost` 最终业务口径、`parent_id=-1` 是否计为业务组数、停用项目默认过滤仍需业务方拍板。

## 状态解释

- Sprint-25 整体仍是 `IN_PROGRESS`,因为业务口径决策未关闭。
- F0 保持 `IN_PROGRESS`,内部 T03 保持 `BLOCKED`。
- F1/F2/F3 从非标准 `DONE_BASELINE` 规范为 `DONE`:这些是工程 baseline 完成,不代表 F0-T03 的最终业务口径已拍板。

## 改后

`sprint-queue.md` 中 S25 已改为:

| Feature | 改后状态 |
|---------|----------|
| F0-项目域P0数据画像与口径决策 | IN_PROGRESS |
| F1-共享维度与项目域dbt建模 | DONE |
| F2-项目域NL2SQL接入 | DONE |
| F3-项目域回归与验收 | DONE |

统计:`READY=0, IN_PROGRESS=1, DONE=3, BLOCKED=0`。
