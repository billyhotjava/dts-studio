# Task 4.1: dts-data-security — Data Exit Checkpoint

**Sprint:** 4 — Security & Audit
**Points:** 8
**Status:** TODO
**Iron Law:** #3 — Data Security Is DNA

## Responsibility
- Column-level policy: allow / mask / deny per user per column
- Row-level filter: generates SQL WHERE clause for tenant isolation + row-level security
- Dynamic masking: email (a***@b.com), phone (138****0000), ID card, custom patterns
- Data watermarking for export tracing
- Classification registry (public / internal / confidential / restricted)

## Key Design

```java
public record ColumnPolicy(String column, PolicyAction action, MaskingRule rule) {}
public enum PolicyAction { ALLOW, MASK, DENY }

public class ColumnPolicyEngine {
    public List<ColumnPolicy> evaluate(
            String userId, String tenantId,
            String objectType, List<String> columns) {
        // classification × user_permission → policy_action
    }
}

public class RowFilterEngine {
    public String generateFilter(String userId, String tenantId, String objectType) {
        // Returns SQL WHERE clause: "tenant_id = 't1' AND department IN ('eng')"
    }
}
```

## Steps

1. Write ColumnPolicyEngine tests (all combinations) → implement
2. Write RowFilterEngine tests → implement
3. Write MaskingEngine tests (each pattern) → implement
4. Implement DataSecurityGrpcService
5. Integration tests
6. Commit

```bash
git commit -m "feat(s4): implement dts-data-security — Iron Law 3 data checkpoint"
```
