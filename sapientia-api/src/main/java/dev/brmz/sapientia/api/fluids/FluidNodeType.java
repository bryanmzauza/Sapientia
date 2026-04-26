package dev.brmz.sapientia.api.fluids;

/**
 * Roles a fluid node can play in a {@code FluidNetwork} (T-301 / 1.2.0).
 *
 * <ul>
 *   <li>{@code PIPE} — passive carrier; no buffer, never produces or consumes.</li>
 *   <li>{@code PUMP} — extracts fluid each tick from an adjacent vanilla source
 *       (cauldron, water/lava block) and pushes it into the network.</li>
 *   <li>{@code DRAIN} — pulls fluid from network buffers and places it in an
 *       adjacent block (cauldron, replaceable air).</li>
 *   <li>{@code TANK} — buffer; holds one {@link FluidStack} bounded by
 *       {@link FluidSpecs#capacityMb}.</li>
 *   <li>{@code JUNCTION} — reserved (no behaviour beyond connectivity).</li>
 * </ul>
 */
public enum FluidNodeType {
    PIPE,
    PUMP,
    DRAIN,
    TANK,
    JUNCTION
}
