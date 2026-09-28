package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Nl2SqlQueryTemplateRepository extends JpaRepository<Nl2SqlQueryTemplate, Long> {

    List<Nl2SqlQueryTemplate> findByIsActiveTrueOrderByPriorityDesc();

    List<Nl2SqlQueryTemplate> findByDomainAndIsActiveTrue(String domain);

    Optional<Nl2SqlQueryTemplate> findByTemplateCode(String templateCode);
}
