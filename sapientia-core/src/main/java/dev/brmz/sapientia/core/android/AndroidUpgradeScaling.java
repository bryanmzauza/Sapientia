package dev.brmz.sapientia.core.android;

/**
 * Pure-POJO scaling tables for the four upgrade kinds (T-454 / 1.9.1).
 *
 * <p>1.9.0 shipped the upgrade items as craftable + persistable but with no
 * observable behavioural effect. 1.9.1 wires the values listed here into
 * {@link AndroidBehaviorEngine}:
 *
 * <ul>
 *   <li>{@code AI_CHIP} → scan radius (blocks) for crop / log / ore /
 *       water lookups.</li>
 *   <li>{@code MOTOR} → cooldown (ticks) between successive instructions.
 *       The {@link AndroidTicker#INSTRUCTIONS_PER_TICK_CAP} contract
 *       (T-451) still holds: motor never raises the per-tick budget; it
 *       only shortens the gap between ticks where work happens.</li>
 *   <li>{@code ARMOUR} → max HP + flat damage reduction. The 1.9.x cycle
 *       does not yet expose damage events to androids, so the values are
 *       documented + tested but read by addons via {@code AndroidNode}.</li>
 *   <li>{@code FUEL_MODULE} → fuel buffer capacity (mb of biofuel /
 *       equivalent). The conversion is fixed at
 *       {@link #BIOFUEL_SU_RATIO} regardless of tier.</li>
 * </ul>
 *
 * <p>Tier values are clamped to {@code [1, 4]} (matching
 * {@code AndroidUpgrade}'s constructor). Out-of-range tiers fall back to
 * tier 1 to keep callers crash-free; logging happens at the call site.
 */
public final class AndroidUpgradeScaling {

    private AndroidUpgradeScaling() {}

    /** mb of biofuel consumed per Sapientia Unit produced. Locked across tiers. */
    public static final long BIOFUEL_SU_RATIO = 100L;

    /** Per-instruction biofuel cost (mb). Drained from the node's fuel buffer. */
    public static final long BIOFUEL_PER_INSTRUCTION = 10L;

    /** Scan radius in blocks for the AI chip tier. */
    public static int chipScanRadius(int tier) {
        return switch (clamp(tier)) {
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 13;
            default -> 4;
        };
    }

    /** Ticks of cooldown enforced between successive android actions. */
    public static int motorCooldownTicks(int tier) {
        return switch (clamp(tier)) {
            case 1 -> 20;
            case 2 -> 14;
            case 3 -> 9;
            case 4 -> 5;
            default -> 20;
        };
    }

    /** Maximum HP for the armour-plate tier. */
    public static int armourMaxHp(int tier) {
        return switch (clamp(tier)) {
            case 1 -> 100;
            case 2 -> 200;
            case 3 -> 400;
            case 4 -> 800;
            default -> 100;
        };
    }

    /** Flat damage absorbed per incoming hit. */
    public static int armourDamageReduction(int tier) {
        return switch (clamp(tier)) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 4;
            default -> 0;
        };
    }

    /** Fuel buffer capacity in mb-equivalent. */
    public static long fuelBufferMax(int tier) {
        return switch (clamp(tier)) {
            case 1 -> 1_000L;
            case 2 -> 4_000L;
            case 3 -> 16_000L;
            case 4 -> 64_000L;
            default -> 1_000L;
        };
    }

    private static int clamp(int tier) {
        if (tier < 1) return 1;
        if (tier > 4) return 4;
        return tier;
    }
}
