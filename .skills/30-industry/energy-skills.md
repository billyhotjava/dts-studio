# Energy Industry Skills

Status: PLACEHOLDER — to be implemented in energy-pack

## Skills

| Name | Type | Description | Priority |
|------|------|-------------|----------|
| energy/predict-load | LLM | Predict transformer/line load based on historical + weather data | P1 |
| energy/detect-defect | Hybrid | Detect equipment defect patterns from SCADA data | P0 |
| energy/create-workorder | Workflow | Create maintenance work order with HITL approval | P0 |
| energy/assess-risk | LLM | Assess grid section risk based on multiple indicators | P1 |
| energy/generate-safety-ticket | Workflow | Generate safety work ticket with multi-level approval | P0 |
| energy/report-reliability | Hybrid | Generate supply reliability (ASAI) report | P1 |

## Domain Constraints (from .rules/50-appstack/industry/energy.rules)

- All actions on live equipment: risk_level high, mandatory HITL.
- SCADA data access filtered by regional authority.
- Defect classification follows DL/T 741 standard.
