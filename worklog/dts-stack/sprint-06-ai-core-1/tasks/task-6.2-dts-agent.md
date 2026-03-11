# Task 6.2: dts-agent — LangGraph Agent Runtime

**Sprint:** 6 — AI Core 1 | **Points:** 13 | **Status:** TODO

## Responsibility
- Agent runtime using LangGraph
- Intent → ExecutionPlan (multi-step planning)
- Step-by-step plan execution (invoke Skills via gRPC)
- Persona loading (system prompt + skill preferences + ontology scope)
- HITL integration (pause at high-risk, create PendingAction via dts-workflow)
- Streaming responses (SSE to frontend)
- Conversation memory management

## Key Design — LangGraph State Machine

```python
class AgentState(TypedDict):
    intent: Intent
    plan: ExecutionPlan
    current_step: int
    results: list[StepResult]
    needs_approval: bool
    messages: list[BaseMessage]

def build_agent_graph() -> StateGraph:
    graph = StateGraph(AgentState)
    graph.add_node("plan", planner_node)
    graph.add_node("execute_step", executor_node)
    graph.add_node("check_approval", approval_check_node)
    graph.add_node("wait_approval", approval_wait_node)
    graph.add_node("summarize", summary_node)
    graph.set_entry_point("plan")
    graph.add_edge("plan", "execute_step")
    graph.add_conditional_edges("execute_step", should_continue, {
        "next_step": "execute_step",
        "needs_approval": "check_approval",
        "done": "summarize",
        "error": "summarize",
    })
    graph.add_conditional_edges("check_approval", check_risk, {
        "auto_approve": "execute_step",
        "human_review": "wait_approval",
    })
    graph.add_edge("wait_approval", "execute_step")
    graph.add_edge("summarize", END)
    return graph.compile()
```

## Files
- `dts-stack/ai/dts-agent/src/dts_agent/server.py`
- `dts-stack/ai/dts-agent/src/dts_agent/graph.py`
- `dts-stack/ai/dts-agent/src/dts_agent/planner.py`
- `dts-stack/ai/dts-agent/src/dts_agent/executor.py`
- `dts-stack/ai/dts-agent/src/dts_agent/persona.py`
- `dts-stack/ai/dts-agent/src/dts_agent/tools/`
- `dts-stack/ai/dts-agent/src/dts_agent/memory/`
- `dts-stack/ai/dts-agent/tests/`

## Steps
1. Design agent state and graph topology
2. Write planner tests → implement
3. Write executor tests (mock skill invocation) → implement
4. Write persona loading tests → implement
5. Write HITL flow tests (approval interception) → implement
6. Implement gRPC server with SSE streaming
7. Integration tests
8. Commit
