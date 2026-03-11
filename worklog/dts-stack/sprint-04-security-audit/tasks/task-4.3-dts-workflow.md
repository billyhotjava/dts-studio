# Task 4.3: dts-workflow — HITL Approval Engine

**Sprint:** 4 — Security & Audit
**Points:** 8
**Status:** TODO

## Responsibility
- Approval workflow management
- HITL: Agent pauses at high-risk actions, creates PendingAction, waits for human
- Risk-level-based routing: medium → team lead, high → manager
- Notification via SSE + Kafka
- Auto-expire pending approvals (configurable timeout per risk level)
- Status lifecycle: pending → approved/rejected/expired

## DB Schema

```sql
CREATE TABLE workflows (
    workflow_id VARCHAR(36) PRIMARY KEY,
    workflow_type VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    config JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'active'
);

CREATE TABLE approval_requests (
    action_id VARCHAR(36) PRIMARY KEY,
    workflow_id VARCHAR(36) REFERENCES workflows(workflow_id),
    intent_id VARCHAR(36),
    plan_id VARCHAR(36),
    step_number INT,
    skill_name VARCHAR(100),
    action_description TEXT,
    risk_level VARCHAR(20) NOT NULL,
    risk_explanation TEXT,
    affected_objects JSONB DEFAULT '[]',
    requested_by VARCHAR(36) NOT NULL,
    approver VARCHAR(36),
    status VARCHAR(20) DEFAULT 'pending',
    timeout_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

## Steps

1. Write approval lifecycle tests → implement
2. Write timeout mechanism tests → implement
3. Write notification tests → implement
4. Implement WorkflowGrpcService
5. Integration tests
6. Commit

```bash
git commit -m "feat(s4): implement dts-workflow with HITL approval engine"
```
