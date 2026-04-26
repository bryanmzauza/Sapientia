package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier rolling mill (T-423 / 1.6.0). Rolls plates → coils, plates → wires faster. */
public final class SapientiaRollingMill extends MachineEnergyBlock {
    public SapientiaRollingMill(@NotNull Plugin plugin) {
        super(plugin, "rolling_mill", Material.SMITHING_TABLE,
                "block.rolling_mill.name", EnergyNodeType.CONSUMER, EnergyTier.HIGH);
    }
}
