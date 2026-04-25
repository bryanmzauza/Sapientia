package dev.brmz.sapientia.content.gas;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier liquefier (T-426 / 1.6.0). Cryogenically liquefies a gas into a fluid. */
public final class SapientiaLiquefier extends MachineEnergyBlock {
    public SapientiaLiquefier(@NotNull Plugin plugin) {
        super(plugin, "liquefier", Material.PACKED_ICE,
                "block.liquefier.name", EnergyNodeType.CONSUMER, EnergyTier.HIGH);
    }
}
