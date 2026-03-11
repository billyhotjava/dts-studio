# Task 12.1: E2E Integration Tests

**Sprint:** 12 — Integration | **Points:** 8 | **Status:** TODO

## 5 Critical Paths

1. **Auth → Query → Security**: User login → JWT → query data → dts-data-security masking → response
2. **Intent → Plan → Execute → Audit**: NL input → parse intent → generate plan → execute skills → audit trail complete
3. **HITL Approval**: High-risk action → PendingAction created → approver notified → approved → executed → audited
4. **Ontology Lifecycle**: Create ObjectType → add instances → lineage tracking → impact analysis
5. **AppPack Lifecycle**: Pack install → skill register → invoke skill → audit → pack uninstall

## Steps
1. Set up test environment (docker-compose all services)
2. Implement critical path 1 → verify
3. Implement critical path 2 → verify
4. Implement critical path 3 → verify
5. Implement critical path 4 → verify
6. Implement critical path 5 → verify
7. Commit
