package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.domain.BizEnumDictionary;
import com.yuzhi.dts.copilot.ai.repository.BizEnumDictionaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for querying business enum dictionaries.
 * Provides enum code-to-label mappings that can be injected into LLM prompts
 * so the AI copilot understands what numeric status codes mean.
 */
@Service
public class BizEnumService {

    private static final Logger log = LoggerFactory.getLogger(BizEnumService.class);

    private final BizEnumDictionaryRepository bizEnumDictionaryRepository;

    public BizEnumService(BizEnumDictionaryRepository bizEnumDictionaryRepository) {
        this.bizEnumDictionaryRepository = bizEnumDictionaryRepository;
    }

    /**
     * Get all enum entries for a specific table and field.
     *
     * @param tableName the database table name, e.g. "p_project"
     * @param fieldName the column name, e.g. "status"
     * @return list of matching enum dictionary entries ordered by sort_order
     */
    public List<BizEnumDictionary> getEnumsForTable(String tableName, String fieldName) {
        return bizEnumDictionaryRepository.findByTableNameAndFieldName(tableName, fieldName);
    }

    /**
     * Look up the display label for a specific enum code.
     *
     * @param tableName the database table name
     * @param fieldName the column name
     * @param code      the enum code value
     * @return the Chinese display label, or empty if not found
     */
    public Optional<String> getLabel(String tableName, String fieldName, String code) {
        List<BizEnumDictionary> enums = bizEnumDictionaryRepository.findByTableNameAndFieldName(tableName, fieldName);
        return enums.stream()
            .filter(e -> code.equals(e.getCode()))
            .map(BizEnumDictionary::getLabel)
            .findFirst();
    }

    /**
     * Get all active enums grouped by "tableName.fieldName" with code-to-label maps.
     *
     * @return map where key is "tableName.fieldName" and value is a code-to-label map
     */
    public Map<String, Map<String, String>> getAllActiveEnums() {
        List<BizEnumDictionary> allActive = bizEnumDictionaryRepository.findByIsActiveTrue();
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (BizEnumDictionary entry : allActive) {
            String key = entry.getTableName() + "." + entry.getFieldName();
            result.computeIfAbsent(key, k -> new LinkedHashMap<>())
                .put(entry.getCode(), entry.getLabel());
        }
        return result;
    }

    /**
     * Format all enum definitions for a given table as readable context for LLM prompt injection.
     * <p>
     * Example output:
     * <pre>
     * Enum definitions for table p_project:
     *   status: 1=正常, 2=停用
     *   type: 1=租摆, 2=节日摆
     *   check_cycle: 1=每月, 2=双月, 3=季度, 6=半年, 12=年度
     * </pre>
     *
     * @param tableName the database table name
     * @return formatted string, or empty string if no enums found for this table
     */
    public String formatEnumsAsContext(String tableName) {
        List<BizEnumDictionary> allActive = bizEnumDictionaryRepository.findByIsActiveTrue();
        Map<String, List<BizEnumDictionary>> fieldGroups = allActive.stream()
            .filter(e -> tableName.equals(e.getTableName()))
            .collect(Collectors.groupingBy(BizEnumDictionary::getFieldName, LinkedHashMap::new, Collectors.toList()));

        if (fieldGroups.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Enum definitions for table ").append(tableName).append(":\n");
        for (Map.Entry<String, List<BizEnumDictionary>> entry : fieldGroups.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ");
            String values = entry.getValue().stream()
                .map(e -> e.getCode() + "=" + e.getLabel())
                .collect(Collectors.joining(", "));
            sb.append(values).append("\n");
        }
        return sb.toString();
    }
}
