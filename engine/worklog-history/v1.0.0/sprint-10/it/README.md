# Sprint-10 IT

本目录用于保存 `Sprint-10: 园林业务语义层与双通道 NL2SQL` 的集成测试计划和验收矩阵。

## 目标

验证以下双通道链路在 `adminapi/adminweb + dts-copilot` 环境中可工作且受控：

- 明细追问 -> 直连只读库
- 指标/趋势 -> 轻量 ELT 主题层

## 覆盖范围

- 主题域识别：项目履约、现场业务
- 查询类型识别：明细、指标、趋势、诊断
- 语义资产：对象字典、同义词、指标口径、join contract
- 权限控制：analytics 执行权限、allowed tables、allowed joins

## 测试资产

- `BG-10-dual-channel-nl2sql-matrix.md`：验收矩阵和示例问句

## 约束

- 不把全部历史业务场景都纳入 v1
- 优先覆盖高价值、可验证、可复现的问句
