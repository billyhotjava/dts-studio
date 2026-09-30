package com.yuzhi.dts.copilot.ai.service.pack;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Collects actual read dependencies on the synchronous agent execution thread.
 * Open inside the streaming worker, never on the servlet thread before handing off.
 * Nested executions are isolated; no state is inherited by background threads.
 */
public final class PackReadScope implements AutoCloseable {
    private static final ThreadLocal<PackReadScope> CURRENT = new ThreadLocal<>();
    private final PackReadScope previous;
    private final LinkedHashSet<PackSourceRef> sources = new LinkedHashSet<>();
    private boolean closed;

    private PackReadScope() {
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static PackReadScope open() {
        return new PackReadScope();
    }

    public static void record(Collection<PackSourceRef> refs) {
        PackReadScope scope = CURRENT.get();
        if (scope != null) scope.sources.addAll(refs);
    }

    public static List<PackSourceRef> currentSources() {
        PackReadScope scope = CURRENT.get();
        return scope == null ? List.of() : scope.sources();
    }

    public List<PackSourceRef> sources() {
        return List.copyOf(sources);
    }

    @Override
    public void close() {
        if (closed) return;
        if (CURRENT.get() != this) throw new IllegalStateException("Pack scopes must close on their owning thread in order");
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
        closed = true;
    }
}
