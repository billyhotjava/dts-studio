# AA-05: 安全防护抽取（SQL 沙箱 + 权限过滤 + 审计）

**状态**: READY
**依赖**: AA-03, AA-04

## 目标

从 dts-platform 抽取安全防护体系，包括 SQL 沙箱、GuardrailsInterceptor、权限过滤和操作审计。

## 技术设计

### 来源文件

- `dts-platform/service/ai/safety/GuardrailsInterceptor.java`
- `dts-platform/service/ai/safety/SqlSandbox.java`（如有独立文件）
- `dts-platform/service/ai/pipeline/PermissionFilter.java`
- `dts-platform/service/ai/pipeline/AuditFilter.java`

### SQL 沙箱规则

```java
// 阻止的 SQL 操作
BLOCKED_KEYWORDS = {"INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE"};

// 白名单：仅允许 SELECT 和 WITH (CTE)
boolean isSafe(String sql) {
    String upper = sql.trim().toUpperCase();
    return upper.startsWith("SELECT") || upper.startsWith("WITH");
}
```

### 审计日志

```sql
CREATE TABLE copilot_ai.ai_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(128),
    action          VARCHAR(64) NOT NULL,
    model           VARCHAR(128),
    input_summary   TEXT,
    output_summary  TEXT,
    tokens_used     INTEGER,
    duration_ms     BIGINT,
    status          VARCHAR(16),
    created_at      TIMESTAMP DEFAULT NOW()
);
```

## 影响文件

- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/safety/GuardrailsInterceptor.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/safety/SqlSandbox.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/service/audit/AiAuditService.java`（新建）
- `dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/domain/AiAuditLog.java`（新建）
- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_003__ai_audit.xml`（新建）

## 完成标准

- [ ] SQL 沙箱阻止 INSERT/UPDATE/DELETE/DROP 等操作
- [ ] 只允许 SELECT 和 WITH 查询通过
- [ ] 所有 AI 操作记录审计日志
- [ ] GuardrailsInterceptor 在 Tool 执行前拦截危险操作
