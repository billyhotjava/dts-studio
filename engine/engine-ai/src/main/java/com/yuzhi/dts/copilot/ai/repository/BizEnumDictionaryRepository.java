package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.BizEnumDictionary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BizEnumDictionaryRepository extends JpaRepository<BizEnumDictionary, Long> {

    List<BizEnumDictionary> findByTableNameAndFieldName(String tableName, String fieldName);

    List<BizEnumDictionary> findBySourceDbAndTableNameAndFieldName(String sourceDb, String tableName, String fieldName);

    List<BizEnumDictionary> findByIsActiveTrue();
}
