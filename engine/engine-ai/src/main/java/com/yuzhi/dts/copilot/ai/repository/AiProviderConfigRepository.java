package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AiProviderConfig.
 */
@Repository
public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, Long> {

    List<AiProviderConfig> findByEnabledTrueOrderByPriorityAsc();

    Optional<AiProviderConfig> findByIsDefaultTrue();

    List<AiProviderConfig> findByProviderType(String providerType);

    boolean existsByName(String name);
}
