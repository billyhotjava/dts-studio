# BG-09: 查询权限桥接

**优先级**: P2
**状态**: READY
**依赖**: BG-02, BG-07

## 目标

确保 Copilot 生成的 SQL 查询受 `analytics` 侧权限控制，不允许绕过权限直接执行任意 SQL，逐步将"最终执行权"从 `ai` 裸 JDBC 工具收回到 `analytics`。

## 技术设计

### 当前架构问题

现有链路：
```
用户问句 → copilot-ai (NL2SQL) → 裸 JDBC 执行 → 返回结果
```

问题：
- `copilot-ai` 拥有业务库的 JDBC 连接，可执行任意 SELECT
- SqlSandbox 只做语法级拦截（禁 DML/DDL），不做业务级权限控制
- 用户 A 可以查到用户 B 负责的项目数据

### 目标架构

```
用户问句 → copilot-ai (NL2SQL 生成 SQL)
              → 发送 SQL + 用户上下文 到 copilot-analytics
              → analytics 做权限检查 + 执行 + 返回
```

### 权限桥接层设计

#### 1. SQL 执行收口

新建 `analytics` 侧 API：

```
POST /api/query/execute-copilot
Headers:
  X-DTS-User-Id: {userId}
  X-DTS-User-Name: {userName}
Body:
  {
    "sql": "SELECT ... FROM v_project_overview WHERE ...",
    "datasourceId": "xxx",
    "source": "copilot-nl2sql",
    "routedDomain": "project"
  }
```

#### 2. 视图级权限

基于视图层实现粗粒度权限：

| 视图 | 权限策略 |
|------|---------|
| v_project_overview | 所有登录用户可查 |
| v_flower_biz_detail | 所有登录用户可查 |
| v_project_green_current | 所有登录用户可查 |
| v_monthly_settlement | 财务角色 / 管理者角色 |
| v_task_progress | 所有登录用户可查 |
| v_curing_coverage | 所有登录用户可查 |
| v_pendulum_progress | 所有登录用户可查 |

#### 3. 行级权限（v2 预留）

当前 v1 视图层不做行级过滤。v2 考虑：
- 项目经理只能查自己负责的项目
- 养护人只能查自己负责的摆位
- 通过 `WHERE manager_name = :current_user` 自动注入

### 4. 非视图查询拦截

如果 NL2SQL 生成的 SQL 引用了非视图表（原始业务表），在 `analytics` 执行前拦截：

```java
// 白名单检查
Set<String> allowedSources = Set.of(
    "v_project_overview", "v_flower_biz_detail", "v_project_green_current",
    "v_monthly_settlement", "v_task_progress", "v_curing_coverage", "v_pendulum_progress"
);

// 解析 SQL 中的表名，检查是否都在白名单中
List<String> referencedTables = SqlParser.extractTableNames(sql);
for (String table : referencedTables) {
    if (!allowedSources.contains(table)) {
        throw new ForbiddenQueryException("不允许查询表: " + table);
    }
}
```

### 审计日志

每次 Copilot 查询记录到 `ai_audit_log`：
- 用户信息
- 原始问句
- 路由域
- 生成的 SQL
- 执行结果（成功/拒绝/错误）
- 执行耗时

## 迁移策略

1. **Phase 1**（本 Sprint）：analytics 新增 execute-copilot API，copilot-ai 优先调用；旧 JDBC 工具保留但标记 deprecated
2. **Phase 2**（后续 Sprint）：移除 copilot-ai 的裸 JDBC 执行能力，所有查询走 analytics

## 完成标准

- [ ] `analytics` 侧 execute-copilot API 实现
- [ ] 视图白名单拦截：非视图表查询被拒绝
- [ ] 结算域视图权限检查（角色限制）
- [ ] 审计日志记录完整
- [ ] copilot-ai 的 NL2SQL 调用链路切换到 analytics 执行
- [ ] 旧 JDBC 工具标记 deprecated
