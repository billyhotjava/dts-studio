# T02: 明细级对账 harness（copilot vs L2 端点逐额）

**优先级**: P0
**状态**: IN_PROGRESS
**依赖**: T01

## 目标

建立可重跑的明细级对账：取一批真实业务单，copilot 的行级查询结果与 oracle 报表端点（L2）逐行逐额相等——这是 req#1 一致性的硬证据。

## 技术设计

- **样本集**：从真实数据取代表性业务单号（如截图中 `结算2026060008`、`BX202606030968`，覆盖租摆链 + 售赠坏链 + 凭证来源各类型）。
- **对账流程**：
  1. 调 oracle 端点（T01 注册的 L2）取该单明细 = 期望值。
  2. 让 copilot 对同一单/同一口径出 SQL 取值。
  3. 逐字段比对：金额到分相等、行数相等、归属（项目/客户/月份）一致。
- **容差**：同口径金额**精确到分**，差异即 FAIL（或登记为口径差异）。
- **形式**：`it/test_f1_detail_reconciliation.sh` + 证据 `it/evidence/`，沿用 sprint-30 `it/test_*.sh` 风格。

## 当前进度

- 已落地 `FinanceDetailReconciliationService`：对 oracle/copilot 两路明细做 FULL JOIN 式比较，按 `businessKey + projectId + accountPeriod` 定位差异。
- 已落地 `FinanceDetailReconciliationSampleRegistry`：样本绑定到 `FinanceOracleRegistry` 已注册的 L2 oracle endpoint，避免样本漂移到非权威入口。
- 已落地 `FinanceDetailReconciliationHarness`：同一样本分别调 oracle source client 与 copilot source client，再复用比较器输出失败信息。
- 已落地 `FinanceDetailReconciliationJsonSourceClient`：支持 adminapi 顶层 `rows`、`AjaxResult.data` 单对象，以及 copilot `/api/dataset` 的 `data.rows + cols/results_metadata.columns` 结构，把两边 JSON payload 归一成 `DetailRow`。
- 已落地 `FinanceDetailReconciliationHttpPayloadProvider`：可按样本 `oracleEndpoint` 调 L2 adminapi（GET query / POST JSON），并按 `copilotRequest.database + nativeSql` 调 copilot `/api/dataset`，支持配置化 `Authorization` 与 `Cookie` 透传。
- 2026-06-05：live 预检发现 L2 adminapi 与 copilot analytics 可能需要不同认证；已按 TDD 支持 `oracle-authorization/oracle-cookie` 与 `analytics-authorization/analytics-cookie` 分离配置，并保留旧 `authorization/cookie` 作为双边 fallback。
- 2026-06-05：live oracle auth 预检：`dts-admin` 健康但 `/rs-flowers-base/...` 无凭证返回 401，且 test token 只适用于 `/test/**`，不能作为财务 L2 oracle 证据；详见 `it/evidence/20260605-local/f1-live-oracle-auth-precheck.md`。
- 2026-06-05：按更新后的权限复测 admin 登录链路：`/api/keycloak/auth/login` 可生成有效 admin session，`/api/keycloak/users` 与 `/api/admin/users` 均可 200；但 `/rs-flowers-base/...` 在当前 `dts-stack-dts-admin-1` 仍为 403，确认该路径属于 legacy `adminapi/rs-gateway` 业务入口，当前栈未提供真实 L2 oracle route；详见 `it/evidence/20260605-local/f1-live-oracle-admin-session-route-precheck.md`。
- 2026-06-05：追加运行态复核：当前 `docker ps` 未见 `rs-gateway`、`rs-flowers-base/flowerbase` 容器，`dts-stack-dts-admin-1` 的 Traefik 标签仅发布 `/api`、`/admin/api`、swagger 等路径，未发布 `/rs-flowers-base`；因此权限更新后仍需先提供 legacy 业务入口。
- 2026-06-05：legacy `adminapi/docker/docker-compose.yml` 的 `gateway(7091)` 与 `flowerbase(7095)` 依赖 Nacos/Redis/配置链，`web` 还会发布 `8000:80` 且当前 host 8000 已被 portainer 占用；不能在当前 dts-stack 上无审查整套启动。
- 2026-06-05：已按 TDD 补强 HTTP provider 的 oracle route 误配提示：当 `/rs-flowers-base/...` 返回非 2xx 时，异常会明确提示 `copilot.finance.reconciliation.oracle-base-url` 需要 legacy `adminapi` / `rs-gateway` / `rs-flowers-base` 入口，可使用 `/flowers-dev-api` 或等价 gateway base URL，不要指向当前 `dts-admin /api` 服务。
- 已补齐样本 `copilotRequest.database/nativeSql`：统一使用 `prs.flowerbiz.federated` 联邦查询入口，月结算样本投影 `public.ods_ptr_mysql_a_month_accounting`，售账样本投影 `public.ods_ptr_mysql_a_sale_account + public.ods_ptr_mysql_t_flower_biz_info`。
- 已固化两个代表样本：`month-settlement-js2026060008`（租摆月对账链）与 `sale-account-bx202606030968`（售赠坏链）。
- 已覆盖租摆链 `month-settlement` 的金额列：`receivableTotalAmount`、`netReceiptTotalAmount`、`foldingAfterTotalAmount`、`totalAmount`。
- 已覆盖售赠坏链 `sale-account` 的金额列：`receivableAmount`、`netReceiptsAmount`、`bizAmount`。
- 已有负例：金额差一分、缺 oracle 行、链路混用均输出可复现失败信息。
- 未完成：legacy `adminapi/rs-gateway` 或 `rs-flowers-base` 真实 base URL 配置、analytics 侧 API key 参数配置、真实 L2 + dataset 双路取数跑通；月结算 L2 端点返回动态 `MonthSettlementDataVo`，且 `a_month_accounting` 无独立结算编号列，当前 `businessKey` 作为样本常量投影，需在 live 数据校验时确认接口预览值与 ODS 已落表值的身份/时点是否一致，或改为更稳定的月结算 ID/编号口径。

## 影响范围

- 产出对账脚本 + 样本集 + 证据
- 机器化资产：`governance/finance-detail-reconciliation-samples.v1.json`
- 关联 copilot 财务查询路径（NL2SQL 出 SQL）与 adminapi oracle 端点

## 验证

- [ ] 样本业务单 copilot 明细 == oracle 端点逐额（到分）
- [x] 故意改 copilot 一处口径 → 对账红（harness 有效）
- [x] adminapi/copilot JSON payload 解析适配（L2/dataset 响应结构可归一到 `DetailRow`）
- [x] HTTP payload provider 合同测试（adminapi GET/POST、dataset native query、Authorization/Cookie 透传）
- [x] HTTP payload provider 支持 L2 oracle 与 analytics dataset 分离认证配置
- [x] HTTP payload provider 对 legacy oracle route 误配输出可操作提示
- [x] 样本 `copilotRequest.database/nativeSql` 完整性（可形成 `/api/dataset` native query 请求）
- [x] `it/test_f1_detail_reconciliation.sh`（core comparator + source orchestration + JSON payload adapter + HTTP provider contract + live sample SQL contract；live 环境实跑待接线）

## 完成标准

- [ ] 明细对账 harness 可重跑、样本覆盖三类来源、全绿且差异可解释
