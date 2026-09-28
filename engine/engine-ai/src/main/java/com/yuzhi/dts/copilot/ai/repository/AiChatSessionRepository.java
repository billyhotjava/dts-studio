package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AiChatSession}.
 */
@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    Optional<AiChatSession> findBySessionId(String sessionId);

    List<AiChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);

    List<AiChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    void deleteBySessionId(String sessionId);
}
