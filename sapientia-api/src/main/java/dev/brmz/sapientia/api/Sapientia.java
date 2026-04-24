package dev.brmz.sapientia.api;

import java.util.Objects;

/**
 * Single entry point for the Sapientia API. A provider (the core plugin) is registered
 * at enable time and retrieved by addons via {@link #get()}. See docs/api-spec.md §5.
 */
public final class Sapientia {

    private static volatile SapientiaAPI instance;

    private Sapientia() {}

    public static SapientiaAPI get() {
        SapientiaAPI api = instance;
        if (api == null) {
            throw new IllegalStateException(
                    "Sapientia API not initialized yet. Ensure the Sapientia plugin is loaded "
                            + "before your addon accesses Sapientia.get().");
        }
        return api;
    }

    /** Called by the core plugin at enable time. Not part of the public contract. */
    public static void register(SapientiaAPI api) {
        instance = Objects.requireNonNull(api, "api");
    }

    /** Called by the core plugin at disable time. Not part of the public contract. */
    public static void unregister() {
        instance = null;
    }
}
