package dev.brmz.sapientia.core.energy;

import dev.brmz.sapientia.core.engine.MachineBehavior;
import dev.brmz.sapientia.core.engine.SapientiaEngine;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts a per-machine step that works on an energy node into a
 * {@link MachineBehavior}: the machine runs every {@code period} ticks while
 * the step reports work, and backs off while it is idle.
 */
public final class EnergyMachineBehavior {

    /** One production step; returns whether the machine did any work. */
    @FunctionalInterface
    public interface Step {
        boolean run(@NotNull SimpleEnergyNode node, @NotNull Block block);
    }

    private EnergyMachineBehavior() {}

    public static @NotNull MachineBehavior of(@NotNull SapientiaEngine engine, @NotNull EnergyServiceImpl energy,
                                              int period, @NotNull Step step) {
        return context -> {
            SimpleEnergyNode node = energy.graph().nodeAt(engine.keyOf(context));
            Block block = engine.blockOf(context);
            if (node == null || block == null) {
                return MachineBehavior.idle(period);
            }
            return step.run(node, block) ? period : MachineBehavior.idle(period);
        };
    }
}
