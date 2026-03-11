# AI Agent Skills

Status: PLACEHOLDER — to be implemented in dts-agent + dts-intent-engine

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| agent/route-intent | LLM | Parse NL into structured Intent, select target Skill(s) | P0 |
| agent/plan-steps | LLM | Decompose complex intent into multi-step execution plan | P0 |
| agent/explain-decision | LLM | Explain AI reasoning chain to user in natural language | P0 |
| agent/collect-feedback | Rule | Collect user feedback (thumbs up/down, correction) → Bad Case flywheel | P0 |
| agent/switch-persona | Rule | Switch active Persona (industry context) for conversation | P1 |

## Implementation Notes

- route-intent: core of dts-intent-engine. Maps NL to Intent struct with goal + constraints + target skills.
- plan-steps: LangGraph state machine generates execution plan. Each step = one Skill invocation.
- explain-decision: takes AI Trace tree from dts-observability and generates human-readable explanation.
- collect-feedback: stores in dts-ai-eval for evaluation suite enrichment.
