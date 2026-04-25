package dev.brmz.sapientia.content.chemistry;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier fermenter (T-414 / 1.5.0). Ferments nutrient broth into biogas precursors. */
public final class SapientiaFermenter extends MachineEnergyBlock {
    public SapientiaFermenter(@NotNull Plugin plugin) {
        super(plugin, "fermenter", Material.COMPOSTER,
                "block.fermenter.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
