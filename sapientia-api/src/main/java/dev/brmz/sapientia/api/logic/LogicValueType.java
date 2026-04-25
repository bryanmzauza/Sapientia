package dev.brmz.sapientia.api.logic;

/**
 * Value types carried by ports + parameters of a {@link LogicProgram} node (T-302 / 1.3.0).
 * Kept intentionally tiny — the DAG runtime is non-Turing and operates on simple
 * primitives passed between pure executors.
 */
public enum LogicValueType {
    INT,
    BOOL,
    STRING
}
