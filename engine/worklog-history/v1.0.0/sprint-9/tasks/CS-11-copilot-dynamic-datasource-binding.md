# CS-11 Copilot 动态数据源绑定

## 背景

当前 Copilot 聊天虽然会保存 `dataSourceId`，但工具层没有真正使用这个外部数据源：

- `webapp` 选择的是 analytics 的数据库对象
- `analytics` 直接把这个 ID 透传给 `copilot-ai`
- `copilot-ai` 的 `schema_lookup` / `execute_query` 仍然使用应用主库连接

这会导致 AI 把应用库误当成业务库，出现 MySQL 业务库被解释成空 `public schema` 的问题。

## 目标

- 让 Copilot 通用工具真正使用用户选择的外部数据源
- 避免 analytics 数据库 ID 和 AI 数据源 ID 混用
- 缺少绑定关系时返回明确错误，而不是静默回退

## 范围

- `dts-copilot-analytics` 聊天代理的数据源 ID 解析
- `dts-copilot-ai` 通用工具的数据源连接解析
- MySQL 驱动补齐

## 不在范围

- 园林业务专用工具改造
- 前端数据库选择器重做

## 完成标准

- [x] 发送 Copilot 聊天消息时，analytics 会把选中的数据库解析成 AI `dataSourceId`
- [x] 当 analytics 数据库没有绑定 AI 数据源时，返回 `400`
- [x] `schema_lookup` 使用外部数据源执行，不再使用应用主库
- [x] `execute_query` 使用外部数据源执行，不再使用应用主库
- [x] AI 服务可执行 MySQL 外部数据源查询
- [x] 增加对应单元测试并通过

## 实施记录

- 已新增 analytics 侧 `CopilotChatDataSourceResolver`
- 已新增 AI 侧 `ManagedToolConnectionProvider`
- `schema_lookup` / `execute_query` 已切换为基于 `ToolContext.dataSourceId` 的外部数据源连接
- `dts-copilot-ai` 已补充 MySQL runtime driver

## 验证

- 通过: `mvn -pl dts-copilot-analytics -Dtest=CopilotChatDataSourceResolverTest,CopilotAiClientTest test`
- 通过: `mvn -pl dts-copilot-ai -Dtest=SchemaLookupToolTest,ExecuteQueryToolTest,AiDataSourceServiceTest test`
- 通过: `mvn -pl dts-copilot-ai,dts-copilot-analytics -DskipTests compile`
- 通过: 从当前机器使用 JDBC 直连 `db.weitaor.com`，确认 `rs_cloud_flower` 可见且存在 `163` 张表
- 通过: `copilot-ai` 与 `analytics` 在 `PG_PORT=15432` 下成功启动
- 通过: 将 AI 数据源 `ptr_mysql` 修正为 `jdbc:mysql://db.weitaor.com:3306/rs_cloud_flower`
- 通过: 使用真实登录态调用 `/api/copilot/chat/send`，选择 analytics 数据库 `id=6` 后返回真实业务表
- 通过: AI 会话详情显示 `dataSourceId=14`，确认 `analytics database id -> ai datasource id` 映射生效
