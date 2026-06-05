package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.FinanceReconciliationScorecardSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceReconciliationScorecardSnapshotRepository
        extends JpaRepository<FinanceReconciliationScorecardSnapshot, Long> {

    Optional<FinanceReconciliationScorecardSnapshot> findFirstByOracleBindingIdOrderByCreatedAtDescIdDesc(
            String oracleBindingId);
}
