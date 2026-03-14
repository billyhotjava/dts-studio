package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsUserRepository extends JpaRepository<AnalyticsUser, Long> {

    Optional<AnalyticsUser> findByEmail(String email);
}
