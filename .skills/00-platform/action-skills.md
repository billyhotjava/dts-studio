# Platform Action Skills

Status: PLACEHOLDER — to be implemented in dts-agent

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| action/navigate | Rule | Navigate user to a specific page/view | P0 |
| action/create-workflow | Rule | Create approval workflow instance in dts-workflow | P0 |
| action/notify | Rule | Send notification to user/group | P1 |

## Implementation Notes

- navigate: returns navigation instruction to frontend via SSE. Frontend handles routing.
- create-workflow: creates PendingAction in dts-workflow. Used by HITL approval flow.
- notify: publishes notification event to Kafka. Consumed by notification delivery (email/webhook/in-app).
