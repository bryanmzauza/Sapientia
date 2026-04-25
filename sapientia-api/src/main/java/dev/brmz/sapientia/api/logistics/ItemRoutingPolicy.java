package dev.brmz.sapientia.api.logistics;

/**
 * Strategy used by {@link ItemSolver}-class implementations to pick which
 * consumer receives the next batch of items when multiple consumers in the
 * same network are eligible.
 *
 * <p>The default policy at the network level is {@link #ROUND_ROBIN}; an
 * individual {@link ItemNodeType#FILTER} node may override locally.
 */
public enum ItemRoutingPolicy {
    /** Distribute items evenly between eligible consumers, advancing a cursor. */
    ROUND_ROBIN,
    /** Send first to the consumer with the highest {@link ItemNode#priority()}. */
    PRIORITY,
    /** Send to the first eligible consumer in deterministic graph order; the rest only get spillover. */
    FIRST_MATCH
}
