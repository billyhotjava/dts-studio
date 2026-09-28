package com.yuzhi.dts.copilot.ai.security;

/**
 * ThreadLocal-based holder for the current request's {@link CopilotUserContext}.
 */
public final class CopilotUserContextHolder {

    private static final ThreadLocal<CopilotUserContext> CONTEXT = new ThreadLocal<>();

    private CopilotUserContextHolder() {
    }

    public static void set(CopilotUserContext context) {
        CONTEXT.set(context);
    }

    public static CopilotUserContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
