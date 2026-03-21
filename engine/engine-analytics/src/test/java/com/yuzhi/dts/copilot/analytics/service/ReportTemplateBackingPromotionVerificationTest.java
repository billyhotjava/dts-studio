package com.yuzhi.dts.copilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class ReportTemplateBackingPromotionVerificationTest {

    private static final String RESOURCE_PATH = "config/liquibase/changelog/0044_promote_procurement_summary_fixed_report.xml";
    private static final String RESOURCE_PATH_STOCK = "config/liquibase/changelog/0045_promote_stock_overview_fixed_report.xml";
    private static final String RESOURCE_PATH_FINANCE = "config/liquibase/changelog/0046_promote_finance_settlement_summary_fixed_report.xml";
    private static final String RESOURCE_PATH_LOW_STOCK = "config/liquibase/changelog/0047_promote_low_stock_alert_fixed_report.xml";
    private static final String RESOURCE_PATH_ADVANCE = "config/liquibase/changelog/0048_promote_finance_advance_request_fixed_report.xml";
    private static final String MASTER_PATH = "config/liquibase/master.xml";

    @Test
    void shouldPromoteProcurementSummaryTemplateAfterAnalysisDraftChanges() throws Exception {
        Document changelog = loadDocument(RESOURCE_PATH);
        Document master = loadDocument(MASTER_PATH);

        assertMasterIncludesPromotion(master);
        Element update = findUpdate(changelog, "PROC-SUPPLIER-AMOUNT-RANK");
        assertThat(columnValue(update, "name")).isEqualTo("采购汇总");
        assertThat(columnValue(update, "data_source_type")).isEqualTo("SQL");
        assertThat(columnValue(update, "target_object")).isEqualTo("authority.procurement.purchase_summary");
        assertThat(columnValue(update, "refresh_policy")).isEqualTo("REALTIME");
        assertThat(columnText(update, "spec_json")).contains("\"placeholderReviewRequired\":false");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"purchaseUserId\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"startDate\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"endDate\"");
    }

    @Test
    void shouldPromoteWarehouseStockOverviewTemplateAfterProcurementSummary() throws Exception {
        Document changelog = loadDocument(RESOURCE_PATH_STOCK);
        Document master = loadDocument(MASTER_PATH);

        assertMasterIncludesStockPromotion(master);
        Element update = findUpdate(changelog, "WH-STOCK-OVERVIEW");
        assertThat(columnValue(update, "name")).isEqualTo("库存现量");
        assertThat(columnValue(update, "data_source_type")).isEqualTo("SQL");
        assertThat(columnValue(update, "target_object")).isEqualTo("authority.inventory.stock_overview");
        assertThat(columnValue(update, "refresh_policy")).isEqualTo("REALTIME");
        assertThat(columnText(update, "spec_json")).contains("\"placeholderReviewRequired\":false");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"storehouseInfoId\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"goodType\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"goodName\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"goodSpecs\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"passNumber\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"underNumber\"");
    }

    @Test
    void shouldPromoteFinanceSettlementSummaryTemplateAfterWarehouseStockOverview() throws Exception {
        Document changelog = loadDocument(RESOURCE_PATH_FINANCE);
        Document master = loadDocument(MASTER_PATH);

        assertMasterIncludesFinancePromotion(master);
        Element update = findUpdate(changelog, "FIN-AR-OVERVIEW");
        assertThat(columnValue(update, "name")).isEqualTo("财务结算汇总");
        assertThat(columnValue(update, "category")).isEqualTo("明细");
        assertThat(columnValue(update, "data_source_type")).isEqualTo("SQL");
        assertThat(columnValue(update, "target_object")).isEqualTo("authority.finance.settlement_summary");
        assertThat(columnValue(update, "refresh_policy")).isEqualTo("REALTIME");
        assertThat(columnText(update, "spec_json")).contains("\"placeholderReviewRequired\":false");
        assertThat(columnText(update, "spec_json")).contains("\"databaseName\":\"园林业务库\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"accountPeriod\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"projectId\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"feeUserId\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"status\"");
    }

    @Test
    void shouldPromoteWarehouseLowStockAlertTemplateAfterFinanceSettlementSummary() throws Exception {
        Document changelog = loadDocument(RESOURCE_PATH_LOW_STOCK);
        Document master = loadDocument(MASTER_PATH);

        assertMasterIncludesLowStockPromotion(master);
        Element update = findUpdate(changelog, "WH-LOW-STOCK-ALERT");
        assertThat(columnValue(update, "name")).isEqualTo("库存现量-低库存预警");
        assertThat(columnValue(update, "category")).isEqualTo("预警");
        assertThat(columnValue(update, "data_source_type")).isEqualTo("SQL");
        assertThat(columnValue(update, "target_object")).isEqualTo("authority.inventory.low_stock_alert");
        assertThat(columnValue(update, "refresh_policy")).isEqualTo("REALTIME");
        assertThat(columnText(update, "spec_json")).contains("\"placeholderReviewRequired\":false");
        assertThat(columnText(update, "spec_json")).contains("\"databaseName\":\"园林业务库\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"storehouseInfoId\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"goodType\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"goodName\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"goodSpecs\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"underNumber\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"status\"");
    }

    @Test
    void shouldPromoteFinanceAdvanceRequestTemplateAfterWarehouseLowStockAlert() throws Exception {
        Document changelog = loadDocument(RESOURCE_PATH_ADVANCE);
        Document master = loadDocument(MASTER_PATH);

        assertMasterIncludesAdvancePromotion(master);
        Element update = findUpdate(changelog, "FIN-ADVANCE-REQUEST-STATUS");
        assertThat(columnValue(update, "name")).isEqualTo("预支申请");
        assertThat(columnValue(update, "category")).isEqualTo("状态");
        assertThat(columnValue(update, "data_source_type")).isEqualTo("SQL");
        assertThat(columnValue(update, "target_object")).isEqualTo("authority.finance.advance_request_status");
        assertThat(columnValue(update, "refresh_policy")).isEqualTo("REALTIME");
        assertThat(columnText(update, "spec_json")).contains("\"placeholderReviewRequired\":false");
        assertThat(columnText(update, "spec_json")).contains("\"databaseName\":\"园林业务库\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"code\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"status\"");
        assertThat(columnText(update, "parameter_schema_json")).contains("\"applyUserId\"");
    }

    private static void assertMasterIncludesPromotion(Document master) {
        List<Element> includes = childElementsByLocalName(master.getDocumentElement(), "include");
        int analysisDraftIndex = -1;
        int promotionIndex = -1;
        for (int i = 0; i < includes.size(); i++) {
            String file = includes.get(i).getAttribute("file");
            if ("config/liquibase/changelog/0043_analysis_drafts.xml".equals(file)) {
                analysisDraftIndex = i;
            }
            if (RESOURCE_PATH.equals(file)) {
                promotionIndex = i;
            }
        }
        assertThat(analysisDraftIndex).isGreaterThanOrEqualTo(0);
        assertThat(promotionIndex).isGreaterThan(analysisDraftIndex);
    }

    private static void assertMasterIncludesStockPromotion(Document master) {
        List<Element> includes = childElementsByLocalName(master.getDocumentElement(), "include");
        int procurementPromotionIndex = -1;
        int stockPromotionIndex = -1;
        for (int i = 0; i < includes.size(); i++) {
            String file = includes.get(i).getAttribute("file");
            if (RESOURCE_PATH.equals(file)) {
                procurementPromotionIndex = i;
            }
            if (RESOURCE_PATH_STOCK.equals(file)) {
                stockPromotionIndex = i;
            }
        }
        assertThat(procurementPromotionIndex).isGreaterThanOrEqualTo(0);
        assertThat(stockPromotionIndex).isGreaterThan(procurementPromotionIndex);
    }

    private static void assertMasterIncludesFinancePromotion(Document master) {
        List<Element> includes = childElementsByLocalName(master.getDocumentElement(), "include");
        int stockPromotionIndex = -1;
        int financePromotionIndex = -1;
        for (int i = 0; i < includes.size(); i++) {
            String file = includes.get(i).getAttribute("file");
            if (RESOURCE_PATH_STOCK.equals(file)) {
                stockPromotionIndex = i;
            }
            if (RESOURCE_PATH_FINANCE.equals(file)) {
                financePromotionIndex = i;
            }
        }
        assertThat(stockPromotionIndex).isGreaterThanOrEqualTo(0);
        assertThat(financePromotionIndex).isGreaterThan(stockPromotionIndex);
    }

    private static void assertMasterIncludesLowStockPromotion(Document master) {
        List<Element> includes = childElementsByLocalName(master.getDocumentElement(), "include");
        int financePromotionIndex = -1;
        int lowStockPromotionIndex = -1;
        for (int i = 0; i < includes.size(); i++) {
            String file = includes.get(i).getAttribute("file");
            if (RESOURCE_PATH_FINANCE.equals(file)) {
                financePromotionIndex = i;
            }
            if (RESOURCE_PATH_LOW_STOCK.equals(file)) {
                lowStockPromotionIndex = i;
            }
        }
        assertThat(financePromotionIndex).isGreaterThanOrEqualTo(0);
        assertThat(lowStockPromotionIndex).isGreaterThan(financePromotionIndex);
    }

    private static void assertMasterIncludesAdvancePromotion(Document master) {
        List<Element> includes = childElementsByLocalName(master.getDocumentElement(), "include");
        int lowStockPromotionIndex = -1;
        int advancePromotionIndex = -1;
        for (int i = 0; i < includes.size(); i++) {
            String file = includes.get(i).getAttribute("file");
            if (RESOURCE_PATH_LOW_STOCK.equals(file)) {
                lowStockPromotionIndex = i;
            }
            if (RESOURCE_PATH_ADVANCE.equals(file)) {
                advancePromotionIndex = i;
            }
        }
        assertThat(lowStockPromotionIndex).isGreaterThanOrEqualTo(0);
        assertThat(advancePromotionIndex).isGreaterThan(lowStockPromotionIndex);
    }

    private static Element findUpdate(Document changelog, String templateCode) {
        for (Element update : childElementsByLocalName(changelog.getDocumentElement(), "update")) {
            String where = normalize(updateText(update, "where"));
            if (where.contains("template_code = '" + templateCode.toLowerCase() + "'")) {
                return update;
            }
        }
        throw new AssertionError("Expected update for template " + templateCode);
    }

    private static Document loadDocument(String resourcePath) throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream).as("Expected Liquibase resource to exist at %s", resourcePath).isNotNull();
        try (InputStream input = stream) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(input, StandardCharsets.UTF_8.name());
            document.getDocumentElement().normalize();
            return document;
        }
    }

    private static List<Element> childElementsByLocalName(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element childElement) {
                elements.add(childElement);
            }
        }
        return elements;
    }

    private static String columnValue(Element update, String columnName) {
        NodeList columns = update.getElementsByTagNameNS("*", "column");
        for (int i = 0; i < columns.getLength(); i++) {
            Element column = (Element) columns.item(i);
            if (columnName.equals(column.getAttribute("name"))) {
                return column.getAttribute("value").trim();
            }
        }
        return "";
    }

    private static String columnText(Element update, String columnName) {
        NodeList columns = update.getElementsByTagNameNS("*", "column");
        for (int i = 0; i < columns.getLength(); i++) {
            Element column = (Element) columns.item(i);
            if (columnName.equals(column.getAttribute("name"))) {
                return String.valueOf(column.getTextContent()).trim();
            }
        }
        return "";
    }

    private static String updateText(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }

    private static String normalize(String value) {
        return String.valueOf(value).replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
