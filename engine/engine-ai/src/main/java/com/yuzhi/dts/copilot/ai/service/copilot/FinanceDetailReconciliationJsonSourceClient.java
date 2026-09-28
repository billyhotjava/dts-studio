package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FinanceDetailReconciliationJsonSourceClient {

    private final ObjectMapper objectMapper;

    public FinanceDetailReconciliationJsonSourceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<FinanceDetailReconciliationService.DetailRow> parseOracleRows(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            String payload) {
        return parseRows(sample, payload);
    }

    public List<FinanceDetailReconciliationService.DetailRow> parseCopilotRows(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            String payload) {
        return parseRows(sample, payload);
    }

    public FinanceDetailReconciliationHarness.DetailSourceClient sourceClient(PayloadProvider payloadProvider) {
        return new PayloadDetailSourceClient(Objects.requireNonNull(payloadProvider, "payloadProvider"));
    }

    private List<FinanceDetailReconciliationService.DetailRow> parseRows(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        JsonNode root = readPayload(payload);
        List<String> columns = columnNames(root);
        List<ObjectNode> rowObjects = rowObjects(sample, root, columns);
        List<FinanceDetailReconciliationService.DetailRow> rows = new ArrayList<>(rowObjects.size());
        for (ObjectNode row : rowObjects) {
            rows.add(toDetailRow(sample, row));
        }
        return rows;
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid finance detail reconciliation payload JSON", e);
        }
    }

    private List<ObjectNode> rowObjects(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            JsonNode root,
            List<String> columns) {
        JsonNode rows = firstArray(
                root.path("rows"),
                root.path("data").path("rows"),
                root.path("data"));
        if (rows != null) {
            List<ObjectNode> rowObjects = new ArrayList<>(rows.size());
            for (JsonNode row : rows) {
                rowObjects.add(toRowObject(row, columns));
            }
            return rowObjects;
        }
        JsonNode data = root.path("data");
        if (data.isObject() && !data.has("rows")) {
            return singleDetailRowOrFail(sample, (ObjectNode) data.deepCopy());
        }
        if (root.isObject() && !root.has("rows") && !root.has("data")) {
            return singleDetailRowOrFail(sample, (ObjectNode) root.deepCopy());
        }
        return List.of();
    }

    private ObjectNode toRowObject(JsonNode row, List<String> columns) {
        if (row.isObject()) {
            return (ObjectNode) row.deepCopy();
        }
        ObjectNode rowObject = objectMapper.createObjectNode();
        if (!row.isArray()) {
            return rowObject;
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Finance detail reconciliation dataset row has no column metadata");
        }
        for (int i = 0; i < row.size() && i < columns.size(); i++) {
            rowObject.set(columns.get(i), row.get(i).deepCopy());
        }
        return rowObject;
    }

    private List<ObjectNode> singleDetailRowOrFail(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            ObjectNode row) {
        if (isDetailRow(sample, row)) {
            return List.of(row);
        }
        throw new IllegalArgumentException("Finance detail reconciliation payload does not contain finance detail rows");
    }

    private boolean isDetailRow(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            ObjectNode row) {
        for (String fieldName : List.of(
                "businessKey",
                "bizCode",
                "businessCode",
                "projectId",
                "projectID",
                "accountPeriod",
                "accountPriod",
                "yearAndMonth",
                "month",
                "period")) {
            JsonNode value = field(row, fieldName);
            if (!value.isMissingNode() && !value.isNull() && !value.asText("").isBlank()) {
                return true;
            }
        }
        for (String amountField : sample.amountFields()) {
            JsonNode value = field(row, amountField);
            if (!value.isMissingNode() && !value.isNull()) {
                return true;
            }
        }
        return false;
    }

    private List<String> columnNames(JsonNode root) {
        JsonNode columns = firstArray(
                root.path("data").path("results_metadata").path("columns"),
                root.path("data").path("cols"),
                root.path("results_metadata").path("columns"),
                root.path("cols"),
                root.path("columns"));
        if (columns == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(columns.size());
        for (JsonNode column : columns) {
            String name = text(column, "name", "display_name");
            if (name.isBlank()) {
                name = fieldRefName(column.path("field_ref"));
            }
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private FinanceDetailReconciliationService.DetailRow toDetailRow(
            FinanceDetailReconciliationSampleRegistry.DetailSample sample,
            ObjectNode row) {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (String field : sample.amountFields()) {
            JsonNode value = field(row, field);
            if (!value.isMissingNode() && !value.isNull()) {
                amounts.put(field, decimal(value));
            }
        }
        return new FinanceDetailReconciliationService.DetailRow(
                sample.chain(),
                firstText(row, sample.businessKey(), "businessKey", "bizCode", "businessCode", "code", "id"),
                firstText(row, sample.projectId(), "projectId", "projectID"),
                firstText(row, sample.accountPeriod(), "accountPeriod", "accountPriod", "yearAndMonth", "month", "period"),
                amounts);
    }

    private String firstText(ObjectNode row, String fallback, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = field(row, fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return fallback == null ? "" : fallback;
    }

    private JsonNode field(ObjectNode row, String fieldName) {
        String safeField = fieldName == null ? "" : fieldName;
        JsonNode exact = row.path(safeField);
        if (!exact.isMissingNode()) {
            return exact;
        }
        JsonNode snake = row.path(toSnakeCase(safeField));
        if (!snake.isMissingNode()) {
            return snake;
        }
        JsonNode lower = row.path(safeField.toLowerCase());
        if (!lower.isMissingNode()) {
            return lower;
        }
        return exact;
    }

    private BigDecimal decimal(JsonNode value) {
        if (value.isNumber()) {
            return value.decimalValue();
        }
        String text = value.asText("").trim().replace(",", "");
        if (text.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text);
    }

    private JsonNode firstArray(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private String text(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String fieldRefName(JsonNode fieldRef) {
        if (!fieldRef.isArray() || fieldRef.size() < 2) {
            return "";
        }
        return fieldRef.get(1).asText("");
    }

    private String toSnakeCase(String fieldName) {
        StringBuilder snake = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char ch = fieldName.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    snake.append('_');
                }
                snake.append(Character.toLowerCase(ch));
            } else {
                snake.append(ch);
            }
        }
        return snake.toString();
    }

    public interface PayloadProvider {
        String oraclePayload(FinanceDetailReconciliationSampleRegistry.DetailSample sample);

        String copilotPayload(FinanceDetailReconciliationSampleRegistry.DetailSample sample);
    }

    private final class PayloadDetailSourceClient implements FinanceDetailReconciliationHarness.DetailSourceClient {

        private final PayloadProvider payloadProvider;

        private PayloadDetailSourceClient(PayloadProvider payloadProvider) {
            this.payloadProvider = payloadProvider;
        }

        @Override
        public List<FinanceDetailReconciliationService.DetailRow> fetchOracleRows(
                FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
            return parseOracleRows(sample, payloadProvider.oraclePayload(sample));
        }

        @Override
        public List<FinanceDetailReconciliationService.DetailRow> fetchCopilotRows(
                FinanceDetailReconciliationSampleRegistry.DetailSample sample) {
            return parseCopilotRows(sample, payloadProvider.copilotPayload(sample));
        }
    }
}
