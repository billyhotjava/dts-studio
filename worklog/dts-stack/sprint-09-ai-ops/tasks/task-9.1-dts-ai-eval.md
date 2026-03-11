# Task 9.1: dts-ai-eval — Evaluation Suites

**Sprint:** 9 — AI Ops | **Points:** 8 | **Status:** TODO

## Responsibility
- Evaluation suite management (JSONL datasets per Skill)
- Regression gate: block deployment if accuracy drops below baseline
- Bad Case flywheel: collect failures → human review → improve training
- Accuracy tracking per Skill over time
- A/B evaluation (compare skill versions)

## Steps
1. Design eval dataset format (JSONL) → tests
2. Implement suite runner → tests
3. Implement regression gate logic → tests
4. Implement bad case collector → tests
5. Implement accuracy tracker → tests
6. gRPC server
7. Commit
