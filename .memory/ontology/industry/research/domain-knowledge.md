# Research Institute Domain Knowledge

## Core Business Objects (14)

1. **Product** — 产品/项目, properties: product_code, phase(论证/方案/工程/设计定型/生产定型), product_type
2. **BOMNode** — BOM节点, properties: level(1/2/3), parent_ref, quantity, unit
3. **Component** — 零部件, properties: part_number, classification(关键件/重要件/一般件), material
4. **ProductionOrder** — 生产工单, properties: order_number, product_ref, quantity, planned_date, actual_date
5. **QualityInspection** — 质量检验, properties: inspection_type, inspector, result(pass/fail/conditional), defect_count
6. **NonConformance (NCR)** — 不合格品报告, properties: ncr_number, severity, root_cause, disposition, closure_status
7. **ChangeRequest (ECR)** — 工程变更申请, properties: ecr_number, change_type, impact_scope, status
8. **ChangeOrder (ECO)** — 工程变更令, properties: eco_number, ecr_ref, approved_by, effective_date
9. **TestReport** — 试验报告, properties: test_type(环境/可靠性/性能/EMC/安全/寿命), result, test_conditions
10. **Supplier** — 供应商, properties: supplier_code, qualification_level, audit_status
11. **Material** — 原材料, properties: material_code, specification, supplier_ref, batch_number
12. **ProjectMilestone** — 项目里程碑, properties: milestone_name, planned_date, actual_date, status
13. **ResearchTask** — 研制任务, properties: task_code, assignee, milestone_ref, progress_percent
14. **DesignDocument** — 设计文件, properties: doc_number, version, approval_status, classification_level

## Core Metrics (6)

| Metric | Formula | Target | Alert |
|--------|---------|--------|-------|
| First-Pass Yield (一次交验合格率) | passed_first_time / total_inspected | > 95% | < 90% warning, < 80% critical |
| Production Plan Completion | completed_orders / planned_orders | > 98% | < 90% critical |
| NCR Closure Timeliness | closed_on_time / total_ncrs | 100% | any overdue = warning |
| Milestone On-Time Rate | on_time_milestones / total_milestones | > 90% | < 80% critical |
| Supplier Quality Rating | weighted_score(delivery+quality+audit) | >= 80/100 | < 60 critical |
| Equipment Availability | uptime / (uptime + downtime) | > 95% | < 90% critical |

## Data Sources

- QMS (质量管理系统): Inspections, NCRs, test reports, quality metrics
- PLM (产品全生命周期管理): BOM, design documents, change management, configuration
- MES: Production orders, work-in-progress tracking, shop floor data
- ERP: Procurement, inventory, supplier management, financials

## Regulatory Standards

- GJB 1269A: Quality management for military products
- GJB 9001C: Quality management system for military organizations
- Five-step closure (五步法归零): Positioning → Mechanism → Countermeasure → Validation → Institutionalization

## Key Constraints

- NCR closure MUST follow five-step procedure — AI cannot auto-approve
- All quality-related actions: HITL required
- Data retention: minimum 10 years (国防科工局要求)
- Product design data (BOM, test results): TOP-SECRET classification
- Engineering changes require multi-level approval workflow
