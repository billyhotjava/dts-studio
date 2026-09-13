package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.FinanceWeakPathReconciliationCandidateSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceWeakPathReconciliationCandidateSnapshotRepository
        extends JpaRepository<FinanceWeakPathReconciliationCandidateSnapshot, Long> {

    List<FinanceWeakPathReconciliationCandidateSnapshot> findTop20ByPolicyIdOrderByCreatedAtDescIdDesc(
            String policyId);
}
