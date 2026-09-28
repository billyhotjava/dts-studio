package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.AiChatFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatFeedbackRepository extends JpaRepository<AiChatFeedback, Long> {

    List<AiChatFeedback> findBySessionId(String sessionId);

    List<AiChatFeedback> findByRating(String rating);

    List<AiChatFeedback> findByReason(String reason);
}
