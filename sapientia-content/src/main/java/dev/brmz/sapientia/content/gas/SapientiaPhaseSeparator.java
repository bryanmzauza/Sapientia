package dev.brmz.sapientia.content.gas;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier phase separator (T-426 / 1.6.0). Splits a multi-phase mixture. */
public final class SapientiaPhaseSeparator extends MachineEnergyBlock {
    public SapientiaPhaseSeparator(@NotNull Plugin plugin) {
        super(plugin, "phase_separator", Material.HOPPER,
                "block.phase_separator.name", EnergyNodeType.CONSUMER, EnergyTier.HIGH);
    }
}
