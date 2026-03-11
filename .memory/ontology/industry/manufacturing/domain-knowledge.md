# Manufacturing Industry Domain Knowledge

## Core Business Objects (10)

1. **Machine** — 设备, properties: machine_id, machine_type, manufacturer, install_date, status, location
2. **ProductionLine** — 产线, properties: line_id, line_type, capacity, shift_pattern
3. **WorkOrder** — 生产工单, properties: order_number, product_ref, quantity, priority, status
4. **Product** — 产品, properties: product_code, product_name, bom_ref, specification
5. **QualityInspection** — 质量检验, properties: inspection_point, method, result, inspector
6. **Material** — 物料, properties: material_code, batch_number, supplier_ref, expiry_date
7. **Inventory** — 库存, properties: location, quantity, min_stock, max_stock
8. **Maintenance** — 维保记录, properties: maintenance_type(preventive/corrective), machine_ref, duration, cost
9. **Shift** — 班次, properties: shift_type(day/night/swing), start_time, end_time, operator_count
10. **Operator** — 操作员, properties: employee_id, skill_level, certifications, assigned_line

## Core Metrics (6)

| Metric | Formula | Target | Alert |
|--------|---------|--------|-------|
| OEE | availability × performance × quality | > 85% | < 70% critical |
| Cycle Time | actual_time / standard_time | <= 1.0 ratio | > 1.2 critical |
| Scrap Rate | scrap_count / total_produced | < 2% | > 5% critical |
| MTBF | total_uptime / failure_count | industry benchmark | < 50% benchmark critical |
| MTTR | total_repair_time / repair_count | < 4 hours | > 8 hours critical |
| Inventory Turnover | COGS / avg_inventory | > 12x/year | < 6x warning |

## Data Sources

- MES: Production orders, WIP tracking, cycle times, yield data
- SCADA/PLC: Machine telemetry via OPC-UA (temperature, vibration, speed, energy), sub-second frequency
- ERP: Material planning, procurement, inventory, costing
- WMS (仓储管理): Warehouse operations, pick/pack/ship
- QMS: Inspection results, NCRs, corrective actions

## Data Characteristics

- SCADA/PLC: High-frequency time-series (10ms-1s intervals), stored in ClickHouse
- Single production line: 10M+ records/day from SCADA
- OPC-UA integration required via dts-data-connector adapter
- Real-time alerting needed for critical machine parameters

## Key Constraints

- Machine start/stop commands: HITL mandatory (risk_level: high)
- Quality hold/release: QA manager approval required
- AI production scheduling suggestions: ADVISORY only, production manager confirms
- Production process parameters: CONFIDENTIAL (trade secrets)
- Inventory levels: CONFIDENTIAL (competitive intelligence)
