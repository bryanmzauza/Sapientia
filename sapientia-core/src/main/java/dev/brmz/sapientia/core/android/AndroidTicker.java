package dev.brmz.sapientia.core.android;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.events.SapientiaAndroidTickEvent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Per-tick driver for the android catalogue (T-451 / 1.9.1).
 *
 * <p>The 1.9.0 placeholder fired the {@link SapientiaAndroidTickEvent} on
 * every snapshot iteration but did no real work. 1.9.1 wires the kinetic
 * loop:
 *
 * <ol>
 *   <li>For every loaded android, check the motor cooldown gate
 *       ({@link AndroidUpgradeScaling#motorCooldownTicks(int)}). The gate
 *       reuses the persisted {@code lastTickMs} field as the "next eligible
 *       tick" counter.</li>
 *   <li>Fire the cancellable {@link SapientiaAndroidTickEvent} so addons
 *       can suppress, throttle, or instrument the loop.</li>
 *   <li>Dispatch through {@link AndroidBehaviorEngine#run}, which performs
 *       at most {@link #INSTRUCTIONS_PER_TICK_CAP} instruction per tick
 *       (T-451 hard contract — motor tier shortens the gap between ticks,
 *       it never raises the per-tick budget).</li>
 *   <li>On a successful instruction, re-arm the cooldown and persist the
 *       new fuel / counter state via {@link AndroidServiceImpl#persist}.</li>
 * </ol>
 *
 * <p>Performance contract: the per-tick wall budget is validated by
 * {@code AndroidTickBenchmark} (P-020 / T-459) — 200 androids must fit in
 * the per-tick budget at ≥ 18 TPS.
 */
public final class AndroidTicker {

    /**
     * Hard contract from T-451: every android executes at most one logic
     * instruction per game tick. Higher tiers of the {@code MOTOR} upgrade
     * (T-454) shorten the cooldown <em>between</em> ticks where work
     * happens, but never raise the per-tick budget.
     */
    public static final int INSTRUCTIONS_PER_TICK_CAP = 1;

    private final Logger logger;
    private final AndroidServiceImpl service;
    private final AndroidBehaviorEngine engine;
    private final AtomicLong tickCount = new AtomicLong(0);

    public AndroidTicker(@NotNull Logger logger, @NotNull AndroidServiceImpl service) {
        this(logger, service, new AndroidBehaviorEngine(logger));
    }

    /** Visible-for-tests constructor that lets the engine be replaced with a stub. */
    AndroidTicker(@NotNull Logger logger,
                  @NotNull AndroidServiceImpl service,
                  @NotNull AndroidBehaviorEngine engine) {
        this.logger = logger;
        this.service = service;
        this.engine = engine;
    }

    /** Runs one tick pass over every loaded android. */
    public void tick() {
        long t = tickCount.incrementAndGet();
        List<SimpleAndroidNode> snapshot = service.snapshot();
        if (snapshot.isEmpty()) return;
        for (SimpleAndroidNode node : snapshot) {
            // Motor cooldown gate — skip until the persisted next-tick stamp.
            if (t < node.lastTickMs()) continue;

            SapientiaAndroidTickEvent event =
                    new SapientiaAndroidTickEvent(node, t, INSTRUCTIONS_PER_TICK_CAP);
            try {
                Bukkit.getPluginManager().callEvent(event);
            } catch (IllegalStateException ignored) {
                // Bukkit may not be initialised in tests.
            }
            if (event.isCancelled()) {
                // Cancelled events still re-arm the cooldown so a misbehaving
                // listener cannot starve the snapshot loop.
                node.setLastTickMs(t + AndroidUpgradeScaling.motorCooldownTicks(node.motorTier()));
                continue;
            }

            boolean worked;
            try {
                worked = engine.run(node, t);
            } catch (Throwable thrown) {
                logger.warning("[AndroidTicker] behaviour threw for " + node.type() + ": " + thrown);
                worked = false;
            }
            // Re-arm cooldown either way; an idle android still pays the
            // motor cost so we don't burn CPU re-scanning the same chest
            // every tick.
            node.setLastTickMs(t + AndroidUpgradeScaling.motorCooldownTicks(node.motorTier()));
            if (worked) {
                try {
                    service.persist(node);
                } catch (Throwable thrown) {
                    logger.warning("[AndroidTicker] persist failed: " + thrown);
                }
            }
        }
    }

    public long tickCount() { return tickCount.get(); }
}
