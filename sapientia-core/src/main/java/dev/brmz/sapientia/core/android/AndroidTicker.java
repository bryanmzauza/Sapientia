package dev.brmz.sapientia.core.android;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.events.SapientiaAndroidTickEvent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Per-tick driver for the 1.9.0 android catalogue (T-451).
 *
 * <p>The full kinetic AI loop (crop / log / ore scan, builder pattern
 * execution, slayer melee, trader exchanges, loot simulation) lands in
 * 1.9.1, mirroring the milestone split convention (1.4.0 → 1.4.1,
 * 1.6.0 → 1.6.1, 1.7.0 → 1.7.1, 1.8.0 → 1.8.1).
 *
 * <p>1.9.0 ships the scaffolding: every loaded android receives a
 * cancellable {@link SapientiaAndroidTickEvent} per cycle, with
 * {@code instructionsConsumed = 1} (the per-tick budget contract from
 * T-451). The ticker also bumps an internal counter so external observers
 * (commands, debug overlays) can confirm the loop is alive.
 */
public final class AndroidTicker {

    /**
     * Hard contract from T-451: every android executes at most one logic
     * instruction per game tick. Higher tiers of the {@code MOTOR} upgrade
     * (T-454) raise this cap once the budget envelope is validated by
     * P-020 (T-459) — capped at 1 in 1.9.0.
     */
    public static final int INSTRUCTIONS_PER_TICK_CAP = 1;

    private final Logger logger;
    private final AndroidServiceImpl service;
    private final AtomicLong tickCount = new AtomicLong(0);

    public AndroidTicker(@NotNull Logger logger, @NotNull AndroidServiceImpl service) {
        this.logger = logger;
        this.service = service;
    }

    /** Runs one tick pass over every loaded android. */
    public void tick() {
        long t = tickCount.incrementAndGet();
        List<SimpleAndroidNode> snapshot = service.snapshot();
        if (snapshot.isEmpty()) return;
        for (SimpleAndroidNode node : snapshot) {
            SapientiaAndroidTickEvent event =
                    new SapientiaAndroidTickEvent(node, t, INSTRUCTIONS_PER_TICK_CAP);
            try {
                Bukkit.getPluginManager().callEvent(event);
            } catch (IllegalStateException ignored) {
                // Bukkit may not be initialised in tests.
            }
            if (event.isCancelled()) continue;
            // 1.9.0 placeholder: the AI behaviour ships in 1.9.1. Today the
            // ticker only marks the android as having seen the tick so the
            // future loop can compute deltas without a cold-start.
            node.setLastTickMs(System.currentTimeMillis());
        }
    }

    public long tickCount() { return tickCount.get(); }
}
