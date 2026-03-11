# Task 6.1: dts-intent-engine — NL → Structured Intent

**Sprint:** 6 — AI Core 1 | **Points:** 8 | **Status:** TODO

## Responsibility
- Natural language → structured Intent (LLM-powered)
- Goal extraction: query / action / conditional_action / analysis
- Constraint extraction: max_steps, timeout, approval_level
- Intent → Skill routing (match goals to available skills)

## Key Design

```python
async def parse_intent(raw_input: str, ontology_context: list[ObjectType]) -> Intent:
    prompt = INTENT_PARSE_PROMPT.format(
        input=raw_input,
        available_types=[t.name for t in ontology_context],
        available_metrics=[m.name for m in metrics],
    )
    response = await llm.generate(prompt, response_format=IntentSchema)
    return Intent(raw_input=raw_input, goals=response.goals, constraints=response.constraints)
```

## Files
- `dts-stack/ai/dts-intent-engine/src/dts_intent_engine/server.py`
- `dts-stack/ai/dts-intent-engine/src/dts_intent_engine/parser.py`
- `dts-stack/ai/dts-intent-engine/src/dts_intent_engine/router.py`
- `dts-stack/ai/dts-intent-engine/src/dts_intent_engine/prompt_templates/`
- `dts-stack/ai/dts-intent-engine/tests/test_parser.py`
- `dts-stack/ai/dts-intent-engine/tests/eval/intent_parsing.jsonl`

## Steps
1. Design intent schema and prompt templates
2. Write parser tests with sample intents → implement
3. Write router tests → implement
4. Implement gRPC server
5. Create eval dataset (intent_parsing.jsonl)
6. Run eval, verify baseline accuracy
7. Commit
