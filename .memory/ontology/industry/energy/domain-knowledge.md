# Energy Industry Domain Knowledge

## Core Business Objects (9)

1. **ElectricDevice** — 变压器/开关/断路器, properties: device_type, voltage_level, capacity, install_date, manufacturer
2. **Substation** — 变电站, properties: voltage_class, total_capacity, jurisdiction_area, geo_location
3. **GridLine** — 输配电线路, properties: line_length, pole_count, voltage_level, conductor_type
4. **PowerCustomer** — 用电客户, properties: customer_type(industrial/commercial/residential), contracted_capacity
5. **Defect** — 缺陷(危急/严重/一般 per DL/T 741), properties: severity, device_ref, discovered_date, status
6. **MaintenanceWorkOrder** — 检修工单, properties: order_type, priority, assignee, scheduled_date
7. **SafetyTicket** — 安全作业票, properties: work_type, risk_level, approver_chain, validity_period
8. **PowerIncident** — 电力事故, properties: incident_type, affected_area, outage_duration, customer_impact
9. **RenewableStation** — 新能源场站, properties: energy_type(solar/wind), installed_capacity, grid_connection_point

## Core Metrics (6)

| Metric | Formula | Target | Alert |
|--------|---------|--------|-------|
| ASAI (供电可靠率) | 1 - (total_outage_hours / total_service_hours) | >= 99.9% | < 99.5% critical |
| Line Loss Rate (线损率) | (input_energy - output_energy) / input_energy | <= 5.5% | > 7% critical |
| Transformer Load Rate | current_load / rated_capacity | 60-85% optimal | > 95% critical |
| Defect Clearance Timeliness | cleared_on_time / total_defects | 100% for critical(24h) | any overdue = critical |
| SAIFI (客户停电频率) | total_interruptions / total_customers | industry benchmark | > 2x benchmark critical |
| Renewable Absorption Rate | consumed_renewable / total_renewable_output | >= 90% | < 80% critical |

## Data Sources

- SCADA: Real-time device telemetry (temperature, load, status), OPC-UA protocol, sub-second frequency
- PMS (生产管理系统): Work orders, maintenance records, equipment registry
- OMS (调度管理系统): Grid topology, switching operations, outage management
- GIS: Geospatial data for substations, lines, service areas
- Metering: Customer electricity consumption, AMI smart meter data

## Regulatory Standards

- DL/T 741: Defect classification standard (危急/严重/一般)
- GB/T 31464: Grid operation safety standards
- Safety ticket regulations: Multi-level approval for live equipment work

## Key Constraints

- All actions on live electrical equipment: HITL mandatory
- Grid topology data: CONFIDENTIAL classification
- Customer usage data: CONFIDENTIAL classification
- SCADA data access: filtered by regional authority
