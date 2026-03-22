# RC-05 第二批固定报表接通 — 实现计划

> **For agentic workers:** REQUIRED: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 接通 7 个固定报表的真实 SQL backing，直接查 `rs_cloud_flower` 业务库原始表。

**Architecture:** 复用 `DefaultFixedReportExecutionService` 的 if-else 模式，每个报表一个 executeXxx() 方法。两个 liquibase migration 提升模板种子。扩展 `AuthorityQueryService` 支持新的 domain 前缀。

**Tech Stack:** Java 21, Spring Boot 3.4, Liquibase 4.29, MySQL（业务库）

---

## Task 1: 扩展 AuthorityQueryService 支持新 domain

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/AuthorityQueryService.java`

- [ ] **Step 1: 新增 flowerbiz 和 project 前缀路由**

在 `resolve()` 方法的 `authority.inventory.` 块之后、`authority.` 通用块之前，新增：

```java
if (normalizedTargetObject.startsWith("authority.flowerbiz.")) {
    return new AuthorityAdapter(
            ReportExecutionPlanService.Route.AUTHORITY_SQL,
            adapterKey,
            targetObject);
}
if (normalizedTargetObject.startsWith("authority.project.")) {
    return new AuthorityAdapter(
            ReportExecutionPlanService.Route.AUTHORITY_SQL,
            adapterKey,
            targetObject);
}
```

在 `adapterKey()` 方法中新增：

```java
if (normalizedTargetObject.startsWith("authority.flowerbiz.")) {
    return "authority.flowerbiz";
}
if (normalizedTargetObject.startsWith("authority.project.")) {
    return "authority.project";
}
```

也在末尾 domain 匹配中新增：

```java
if ("报花".equals(domain) || domain.contains("flowerbiz") || domain.contains("flower")) {
    return "authority.flowerbiz";
}
if ("项目".equals(domain) || domain.contains("project")) {
    return "authority.project";
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl dts-copilot-analytics -am -q -DskipTests`

- [ ] **Step 3: Commit**

---

## Task 2: 实现报花单据汇总 + 配送记录 + 日常报销

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionService.java`

- [ ] **Step 1: 在 execute() 方法的 if-else 链中新增 3 个路由**

在 `authority.finance.advance_request_status` 块之后新增：

```java
if ("authority.flowerbiz.biz_document_summary".equals(normalizedTarget)) {
    return Optional.of(executeFlowerBizDocumentSummary(contract, parameters));
}
if ("authority.procurement.delivery_record".equals(normalizedTarget)) {
    return Optional.of(executeProcurementDeliveryRecord(contract, parameters));
}
if ("authority.finance.reimbursement_list".equals(normalizedTarget)) {
    return Optional.of(executeFinanceReimbursementList(contract, parameters));
}
```

- [ ] **Step 2: 实现报花单据汇总**

```java
private ExecutionResult executeFlowerBizDocumentSummary(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT
                a.project_name AS projectName,
                CASE a.biz_type
                    WHEN 1 THEN '加花' WHEN 2 THEN '减花' WHEN 3 THEN '换花'
                    WHEN 4 THEN '调花' WHEN 5 THEN '初摆' WHEN 10 THEN '售花'
                    WHEN 11 THEN '内购' ELSE '其他'
                END AS bizTypeName,
                a.code,
                a.title,
                CASE a.status
                    WHEN 1 THEN '草稿' WHEN 2 THEN '审核中' WHEN 3 THEN '已通过'
                    WHEN 4 THEN '已完成' WHEN -1 THEN '已作废' ELSE '未知'
                END AS statusName,
                CASE a.urgent WHEN 1 THEN '是' ELSE '否' END AS urgentName,
                COALESCE(a.apply_use_name, '') AS applyUserName,
                COALESCE(a.curing_user_name, '') AS curingUserName,
                DATE_FORMAT(a.apply_time, '%Y-%m-%d %H:%i') AS applyTime,
                DATE_FORMAT(a.finish_time, '%Y-%m-%d %H:%i') AS finishTime
            FROM t_flower_biz_info a
            WHERE (a.del_flag IS NULL OR a.del_flag = '0')
            """);
    List<Object> bindings = new ArrayList<>();

    String projectId = stringParam(parameters, "projectId");
    if (projectId != null) {
        sql.append(" AND a.project_id = ?");
        bindings.add(projectId);
    }
    String bizType = stringParam(parameters, "bizType");
    if (bizType != null) {
        sql.append(" AND a.biz_type = ?");
        bindings.add(bizType);
    }
    String status = stringParam(parameters, "status");
    if (status != null) {
        sql.append(" AND a.status = ?");
        bindings.add(status);
    }
    String startDate = stringParam(parameters, "startDate");
    if (startDate != null) {
        sql.append(" AND a.apply_time >= CONCAT(?, ' 00:00:00')");
        bindings.add(startDate);
    }
    String endDate = stringParam(parameters, "endDate");
    if (endDate != null) {
        sql.append(" AND a.apply_time <= CONCAT(?, ' 23:59:59')");
        bindings.add(endDate);
    }

    sql.append(" ORDER BY a.apply_time DESC, a.id DESC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 3: 实现配送记录**

```java
private ExecutionResult executeProcurementDeliveryRecord(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT
                a.code,
                a.title,
                CASE a.status WHEN 1 THEN '配送中' WHEN 2 THEN '已结束' ELSE '未知' END AS statusName,
                CASE a.delivery_mode
                    WHEN 1 THEN '市场->项目点' WHEN 2 THEN '库房->项目点'
                    WHEN 3 THEN '库房->库房' ELSE '其他'
                END AS deliveryModeName,
                COALESCE(a.source_address, '') AS sourceAddress,
                COALESCE(a.destination, '') AS destination,
                COALESCE(a.delivery_user_name, '') AS deliveryUserName,
                COALESCE(a.receive_user_name, '') AS receiveUserName,
                DATE_FORMAT(a.start_delivery_time, '%Y-%m-%d %H:%i') AS deliveryTime,
                DATE_FORMAT(a.receive_time, '%Y-%m-%d %H:%i') AS receiveTime
            FROM t_delivery_info a
            WHERE (a.del_flag IS NULL OR a.del_flag = '0')
            """);
    List<Object> bindings = new ArrayList<>();

    String status = stringParam(parameters, "status");
    if (status != null) {
        sql.append(" AND a.status = ?");
        bindings.add(status);
    }
    String startDate = stringParam(parameters, "startDate");
    if (startDate != null) {
        sql.append(" AND a.start_delivery_time >= CONCAT(?, ' 00:00:00')");
        bindings.add(startDate);
    }
    String endDate = stringParam(parameters, "endDate");
    if (endDate != null) {
        sql.append(" AND a.start_delivery_time <= CONCAT(?, ' 23:59:59')");
        bindings.add(endDate);
    }

    sql.append(" ORDER BY a.start_delivery_time DESC, a.id DESC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 4: 实现日常报销**

```java
private ExecutionResult executeFinanceReimbursementList(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT
                a.code,
                a.title,
                COALESCE(a.apply_user_name, '') AS applyUserName,
                ROUND(COALESCE(a.total_amount, 0), 2) AS totalAmount,
                CASE a.status
                    WHEN 1 THEN '草稿' WHEN 2 THEN '审核中' WHEN 3 THEN '待付款'
                    WHEN 4 THEN '已付款' WHEN -1 THEN '已作废' ELSE '未知'
                END AS statusName,
                CASE a.invoice_status WHEN 1 THEN '无' WHEN 2 THEN '有' ELSE '' END AS invoiceStatusName,
                COALESCE(a.collect_name, '') AS collectName,
                DATE_FORMAT(a.apply_time, '%Y-%m-%d %H:%i') AS applyTime,
                DATE_FORMAT(a.pay_time, '%Y-%m-%d %H:%i') AS payTime
            FROM f_expense_account_info a
            WHERE 1 = 1
            """);
    List<Object> bindings = new ArrayList<>();

    String code = stringParam(parameters, "code");
    if (code != null) {
        sql.append(" AND a.code LIKE ?");
        bindings.add('%' + code + '%');
    }
    String status = stringParam(parameters, "status");
    if (status != null) {
        sql.append(" AND a.status = ?");
        bindings.add(status);
    }
    String applyUserId = stringParam(parameters, "applyUserId");
    if (applyUserId != null) {
        sql.append(" AND a.apply_user_id = ?");
        bindings.add(applyUserId);
    }

    sql.append(" ORDER BY a.apply_time DESC, a.id DESC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl dts-copilot-analytics -am -q -DskipTests`

- [ ] **Step 6: Commit**

---

## Task 3: 实现出入库记录 + 合同管理 + 监管盘点 + 项目点汇总

**Files:**
- Modify: `dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/service/report/DefaultFixedReportExecutionService.java`

- [ ] **Step 1: 在 execute() 的 if-else 链中新增 4 个路由**

```java
if ("authority.inventory.inout_record".equals(normalizedTarget)) {
    return Optional.of(executeWarehouseInOutRecord(contract, parameters));
}
if ("authority.project.contract_list".equals(normalizedTarget)) {
    return Optional.of(executeProjectContractList(contract, parameters));
}
if ("authority.project.supervision_check_summary".equals(normalizedTarget)) {
    return Optional.of(executeProjectSupervisionCheckSummary(contract, parameters));
}
if ("authority.project.fulfillment_summary".equals(normalizedTarget)) {
    return Optional.of(executeProjectFulfillmentSummary(contract, parameters));
}
```

- [ ] **Step 2: 实现出入库记录**

```java
private ExecutionResult executeWarehouseInOutRecord(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT * FROM (
                SELECT '入库' AS direction, wi.code, sh.name AS storehouseName,
                       a.good_name AS goodName, COALESCE(a.good_specs, '') AS goodSpecs,
                       a.good_number AS quantity, ROUND(COALESCE(a.price, 0), 2) AS unitPrice,
                       DATE_FORMAT(wi.warehousing_time, '%Y-%m-%d') AS recordDate
                FROM t_warehousing_item a
                JOIN t_warehousing_info wi ON wi.id = a.warehousing_info_id
                JOIN s_storehouse_info sh ON sh.id = wi.storehouse_info_id
                UNION ALL
                SELECT '出库', ei.code, sh.name,
                       a.good_name, COALESCE(a.good_specs, ''),
                       a.good_number, ROUND(COALESCE(a.price, 0), 2),
                       DATE_FORMAT(ei.ex_warehouse_time, '%Y-%m-%d')
                FROM t_ex_warehouse_item a
                JOIN t_ex_warehouse_info ei ON ei.id = a.ex_warehouse_info_id
                JOIN s_storehouse_info sh ON sh.id = ei.storehouse_info_id
            ) t WHERE 1 = 1
            """);
    List<Object> bindings = new ArrayList<>();

    String goodName = stringParam(parameters, "goodName");
    if (goodName != null) {
        sql.append(" AND t.goodName LIKE ?");
        bindings.add('%' + goodName + '%');
    }
    String startDate = stringParam(parameters, "startDate");
    if (startDate != null) {
        sql.append(" AND t.recordDate >= ?");
        bindings.add(startDate);
    }
    String endDate = stringParam(parameters, "endDate");
    if (endDate != null) {
        sql.append(" AND t.recordDate <= ?");
        bindings.add(endDate);
    }

    sql.append(" ORDER BY t.recordDate DESC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 3: 实现合同管理**

```java
private ExecutionResult executeProjectContractList(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT
                a.code,
                a.title,
                COALESCE(b.name, '') AS customerName,
                CASE a.status WHEN 1 THEN '执行中' WHEN 2 THEN '已结束' ELSE '未知' END AS statusName,
                DATE_FORMAT(a.signing_time, '%Y-%m-%d') AS signDate,
                DATE_FORMAT(a.start_date, '%Y-%m-%d') AS startDate,
                DATE_FORMAT(a.end_date, '%Y-%m-%d') AS endDate,
                COALESCE(a.settlement_period, '') AS settlePeriod,
                COALESCE(a.business_personnel_name, '') AS bizPersonnelName
            FROM p_contract a
            LEFT JOIN p_customer b ON b.id = a.customer_id
            WHERE (a.del_flag IS NULL OR a.del_flag = '0')
            """);
    List<Object> bindings = new ArrayList<>();

    String customerName = stringParam(parameters, "customerName");
    if (customerName != null) {
        sql.append(" AND b.name LIKE ?");
        bindings.add('%' + customerName + '%');
    }
    String status = stringParam(parameters, "status");
    if (status != null) {
        sql.append(" AND a.status = ?");
        bindings.add(status);
    }

    sql.append(" ORDER BY a.signing_time DESC, a.id DESC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 4: 实现监管盘点汇总**

```java
private ExecutionResult executeProjectSupervisionCheckSummary(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT
                a.project_name AS projectName,
                a.title AS checkPeriod,
                COALESCE(a.supervise_user_name, '') AS checkUserName,
                CASE a.status WHEN 1 THEN '盘点中' WHEN 2 THEN '已结束' ELSE '未知' END AS statusName,
                COALESCE(a.total_group_number, 0) AS totalGroupCount,
                COALESCE(a.finish_check_number, 0) AS checkedGroupCount,
                COALESCE(a.total_position_number, 0) AS totalPositionCount,
                COALESCE(a.finishcheck_position_number, 0) AS checkedPositionCount,
                CASE WHEN a.total_position_number > 0
                     THEN ROUND(a.finishcheck_position_number * 100.0 / a.total_position_number, 1)
                     ELSE 0
                END AS progressPct
            FROM t_supervise_check_batch a
            WHERE 1 = 1
            """);
    List<Object> bindings = new ArrayList<>();

    String projectId = stringParam(parameters, "projectId");
    if (projectId != null) {
        sql.append(" AND a.project_id = ?");
        bindings.add(projectId);
    }
    String status = stringParam(parameters, "status");
    if (status != null) {
        sql.append(" AND a.status = ?");
        bindings.add(status);
    }

    sql.append(" ORDER BY a.start_time DESC, a.project_name ASC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 5: 实现项目点经营汇总**

```java
private ExecutionResult executeProjectFulfillmentSummary(QueryContract contract, Map<String, Object> parameters)
        throws SQLException {
    AnalyticsDatabase database = resolveDatabase(contract.databaseName());
    StringBuilder sql = new StringBuilder("""
            SELECT
                p.name AS projectName,
                COALESCE(c.name, '') AS customerName,
                COALESCE(p.manager_name, '') AS managerName,
                COALESCE(p.biz_user_name, '') AS bizUserName,
                CASE p.status WHEN 1 THEN '正常' WHEN 2 THEN '暂停' WHEN 3 THEN '结束' ELSE '未知' END AS statusName,
                COALESCE(ct.title, '') AS contractTitle,
                (SELECT COUNT(*) FROM p_project_green pg WHERE pg.project_id = p.id AND (pg.del_flag IS NULL OR pg.del_flag = '0')) AS greenCount,
                (SELECT COUNT(*) FROM p_position pp WHERE pp.project_id = p.id AND (pp.del_flag IS NULL OR pp.del_flag = '0')) AS positionCount,
                (SELECT COUNT(*) FROM t_flower_biz_info fb WHERE fb.project_id = p.id AND (fb.del_flag IS NULL OR fb.del_flag = '0') AND fb.status = 4) AS completedBizCount
            FROM p_project p
            LEFT JOIN p_contract ct ON ct.id = p.contract_id
            LEFT JOIN p_customer c ON c.id = ct.customer_id
            WHERE (p.del_flag IS NULL OR p.del_flag = '0')
            """);
    List<Object> bindings = new ArrayList<>();

    String projectId = stringParam(parameters, "projectId");
    if (projectId != null) {
        sql.append(" AND p.id = ?");
        bindings.add(projectId);
    }
    String status = stringParam(parameters, "status");
    if (status != null) {
        sql.append(" AND p.status = ?");
        bindings.add(status);
    }

    sql.append(" ORDER BY p.name ASC");

    DatasetResult result = datasetQueryService.runNative(
            database.getId(), sql.toString(),
            new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null), bindings);
    return mapPreview(database, result);
}
```

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl dts-copilot-analytics -am -q -DskipTests`

- [ ] **Step 7: Commit**

---

## Task 4: Liquibase migration — 模板种子提升

**Files:**
- Create: `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/0047_promote_batch2_reports.xml`
- Modify: `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml`

- [ ] **Step 1: 创建 migration 文件**

为 7 个报表创建 INSERT 或 UPDATE 语句，设置正确的 `template_code`、`target_object`、`spec_json`、`parameter_schema_json`。对已种入的 `FIN-REIMBURSEMENT-STATUS` 用 UPDATE，其余 6 个用 INSERT（precondition check）。

- [ ] **Step 2: 注册到 master.xml**

在 `0046_promote_finance_settlement_summary_fixed_report.xml` 之后添加：

```xml
<include file="config/liquibase/changelog/0047_promote_batch2_reports.xml" relativeToChangelogFile="false"/>
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl dts-copilot-analytics -am -q -DskipTests`

- [ ] **Step 4: Commit**

---

## Task 5: IT 验证

- [ ] **Step 1: 重启服务，验证 migration 执行**

- [ ] **Step 2: 用 curl 验证每个报表能返回数据**

```bash
# 登录获取 session
curl -s -c /tmp/adm.txt -X POST http://localhost:8092/api/session \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"Test1234"}'

# 验证报花单据汇总
curl -s -b /tmp/adm.txt http://localhost:8092/api/fixed-reports/BIZ-FLOWER-DOC-SUMMARY/run | head -c 300

# 验证配送记录
curl -s -b /tmp/adm.txt http://localhost:8092/api/fixed-reports/PROC-DELIVERY-RECORD/run | head -c 300

# 其他 5 个类似...
```

- [ ] **Step 3: 验证参数筛选**

```bash
curl -s -b /tmp/adm.txt \
  'http://localhost:8092/api/fixed-reports/BIZ-FLOWER-DOC-SUMMARY/run?bizType=1' | head -c 300
```
