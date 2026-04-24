package dev.brmz.sapientia.api.machine;

/** Lifecycle state of a {@link Machine}. See docs/api-spec.md §2.1. */
public enum MachineState {
    /** Waiting for inputs, power or a recipe. */
    IDLE,
    /** Actively processing a recipe. */
    RUNNING,
    /** Stalled by missing energy, output space or ingredients. */
    BLOCKED,
    /** Manually disabled by a player. */
    DISABLED,
    /** Failed and requires intervention. */
    ERROR
}
