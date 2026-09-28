package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.Nl2SqlRoutingRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Nl2SqlRoutingRuleRepository extends JpaRepository<Nl2SqlRoutingRule, Long> {

    List<Nl2SqlRoutingRule> findByIsActiveTrueOrderByPriorityDesc();
}
