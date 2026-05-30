package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.security.CopilotUserContext;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActionGuardService {

    public GuardDecision verify(String requiredGuard, CopilotUserContext userContext) {
        String guard = normalize(requiredGuard);
        if (!StringUtils.hasText(guard)) {
            return GuardDecision.allowed(null);
        }
        if (userContext == null) {
            return GuardDecision.denied(guard, "缺少用户上下文，无法校验权限: " + guard);
        }
        Set<String> authorities = userContext.roles() == null
                ? Set.of()
                : userContext.roles().stream()
                        .map(ActionGuardService::normalize)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toUnmodifiableSet());
        if (authorities.contains(guard) || hasAdminAuthority(authorities)) {
            return GuardDecision.allowed(guard);
        }
        return GuardDecision.denied(guard, "缺少权限: " + guard);
    }

    private static boolean hasAdminAuthority(Set<String> authorities) {
        return authorities.stream().anyMatch(authority -> {
            String normalized = authority.toLowerCase(Locale.ROOT);
            return "*".equals(authority)
                    || "admin".equals(normalized)
                    || "role_admin".equals(normalized);
        });
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public record GuardDecision(boolean allowed, String requiredGuard, String message) {
        static GuardDecision allowed(String requiredGuard) {
            return new GuardDecision(true, requiredGuard, "ok");
        }

        static GuardDecision denied(String requiredGuard, String message) {
            return new GuardDecision(false, requiredGuard, message);
        }
    }
}
