package dev.brmz.sapientia.content.gas;

import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.api.fluids.FluidNodeType;
import dev.brmz.sapientia.content.fluids.FluidsContentBlockExt;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * HV-tier pressurised gas pipe (T-426 / 1.6.0). Behaves as a regular fluid
 * pipe today (gases share the {@link dev.brmz.sapientia.api.fluids.FluidService}
 * graph per ADR-019); the high-pressure throughput cap arrives with the gas
 * solver pass in 1.6.1.
 */
public final class SapientiaPressurizedPipe extends FluidsContentBlockExt {
    public SapientiaPressurizedPipe(@NotNull Plugin plugin) {
        super(plugin, "pressurized_pipe", Material.IRON_BARS,
                "block.pressurized_pipe.name", FluidNodeType.PIPE, EnergyTier.HIGH);
    }
}
