package dev.brmz.sapientia.api.machine;

import org.jetbrains.annotations.NotNull;

/**
 * Voltage-incompatibility policy helper (T-400 / T-407 / 1.4.0).
 *
 * <p>When two adjacent energy components have mismatched {@link MachineTier},
 * Sapientia decides between three policies (see {@link Policy}):
 * <ul>
 *   <li>{@link Policy#ALLOW} — connection succeeds (used between equal or
 *       transformer-mediated tiers).</li>
 *   <li>{@link Policy#CLAMP} — silent down-throughput to the lower tier.</li>
 *   <li>{@link Policy#BURN} — destructive: cable pops / machine ejects buffer.</li>
 * </ul>
 *
 * <p>Default policy: <strong>BURN</strong> when source tier &gt; target tier
 * (overdriving), <strong>CLAMP</strong> when target tier &gt; source tier
 * (under-feed). Connecting equal tiers always {@link Policy#ALLOW}s. Transformers
 * bypass this check by exposing two endpoints, each on its native tier.
 *
 * <p>See {@code docs/adr-017-voltage-policy.md}.
 */
public final class TierCompatibility {

    private TierCompatibility() {}

    /** Outcome of a tier-to-tier compatibility check. */
    public enum Policy {
        /** Connection valid, full throughput. */
        ALLOW,
        /** Connection valid, throughput silently capped to lower tier's limit. */
        CLAMP,
        /** Connection invalid; cable burns out or machine pops. */
        BURN
    }

    /**
     * Computes the policy for routing energy from {@code source} into {@code target}.
     */
    public static @NotNull Policy check(@NotNull MachineTier source, @NotNull MachineTier target) {
        int delta = source.ordinal() - target.ordinal();
        if (delta == 0) return Policy.ALLOW;
        if (delta > 0) return Policy.BURN;   // overdriving a lower-tier consumer/cable
        return Policy.CLAMP;                  // under-feeding a higher-tier consumer
    }

    /** Convenience: are these two tiers safe to connect at full rate? */
    public static boolean isCompatible(@NotNull MachineTier source, @NotNull MachineTier target) {
        return check(source, target) == Policy.ALLOW;
    }
}
