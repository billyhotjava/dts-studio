package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import java.util.Map;

public interface PlannerPolicy {

    String mode();

    ConversationPlan plan(String userQuestion, Map<String, Boolean> martHealthSnapshot);

    default ConversationPlan plan(String userQuestion, CopilotChatRequestContext requestContext) {
        return plan(
                userQuestion,
                requestContext == null ? Map.of() : requestContext.martHealthSnapshot());
    }
}
