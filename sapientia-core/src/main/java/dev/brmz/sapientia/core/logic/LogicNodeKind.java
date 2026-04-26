package dev.brmz.sapientia.core.logic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import dev.brmz.sapientia.core.logic.builtin.BuiltinKinds;
import org.jetbrains.annotations.NotNull;

/**
 * Process-wide registry of {@link LogicNodeExecutor}s keyed by node kind
 * (T-302 / 1.3.0). Built-in kinds register themselves via {@link BuiltinKinds}
 * lazily on first lookup; addons may register additional kinds at boot before
 * any program is compiled.
 */
public final class LogicNodeKind {

    private static final Map<String, LogicNodeExecutor> EXECUTORS = new LinkedHashMap<>();
    private static volatile boolean bootstrapped;

    private LogicNodeKind() { }

    public static synchronized void register(@NotNull String kind, @NotNull LogicNodeExecutor executor) {
        bootstrap();
        if (kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank");
        }
        EXECUTORS.put(kind, executor);
    }

    public static @NotNull Optional<LogicNodeExecutor> executor(@NotNull String kind) {
        bootstrap();
        return Optional.ofNullable(EXECUTORS.get(kind));
    }

    public static @NotNull Set<String> known() {
        bootstrap();
        synchronized (LogicNodeKind.class) {
            return new TreeSet<>(EXECUTORS.keySet());
        }
    }

    private static void bootstrap() {
        if (bootstrapped) return;
        synchronized (LogicNodeKind.class) {
            if (bootstrapped) return;
            BuiltinKinds.registerAll(EXECUTORS);
            bootstrapped = true;
        }
    }
}
