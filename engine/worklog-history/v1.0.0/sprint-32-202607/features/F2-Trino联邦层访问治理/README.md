# F2: Trino 联邦层访问治理

**优先级**: P0
**状态**: READY

## 目标

给 Trino 联邦层（Sprint-30 F6）补上访问治理，避免"联邦在 lake 规模上重演 tech-debt 里的重查询打业务库"，并接入 Ranger 行列脱敏与资源/审计护栏——使联邦层可放心向多场景铺开。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | Trino→biz MySQL 访问策略（读副本/限流） | P0 | READY | - |
| T02 | Ranger 行列脱敏接入联邦路径 | P0 | READY | T01 |
| T03 | 联邦查询资源/超时/审计护栏 | P1 | READY | S30-F6 |

## Task 明细

### T01 访问策略（读副本/限流）
- **目标**：联邦读 biz MySQL 不直打生产主库、不无限并发。
- **设计**：Trino mysql catalog 指向**读副本**（若有）或受限账号；连接池上限 + 并发/队列限制；明确"联邦只读"。配置在 dts-stack Trino catalog + dts-copilot 连接定义。
- **影响**：dts-stack `dts-trino` catalog 配置；analytics 联邦数据源定义；docker-compose/env。
- **验证**：压一批联邦查询，确认连接数受限、不打主库（连接来源核验）。

### T02 Ranger 行列脱敏
- **目标**：联邦查询遵守与直连一致的行列级权限/脱敏。
- **设计**：Trino 接 Ranger（dts-stack 已有 `dts-ranger`），对 mysql/postgres catalog 应用行过滤 + 列脱敏策略；与 copilot 的角色（role_hint）对齐。
- **影响**：dts-stack Ranger + Trino 集成配置；联邦路径鉴权透传。
- **验证**：受限角色对敏感列查询被脱敏/拒绝，普通列正常。

### T03 资源/超时/审计护栏
- **目标**：扩展 `FederatedQueryGuardrail`，加运行时资源与审计边界。
- **设计**：查询级超时、结果行数上限、扫描量/内存上限（Trino resource group）；联邦查询审计日志（谁、何 catalog、何表、耗时）。在 guardrail 静态校验之外补运行时护栏。
- **影响**：`FederatedQueryGuardrail` + `QueryExecutionFacade`（analytics）；Trino resource group 配置。
- **验证**：超时/超量查询被拦并有审计；正常查询不受影响；analytics 既有联邦测试不回归。

## 完成标准

- [ ] 联邦读走读副本/受限账号 + 连接限流，不直打主库
- [ ] 联邦路径接 Ranger 行列脱敏，与角色对齐
- [ ] 资源/超时/审计护栏生效，guardrail 静态+运行时双层
