package dev.brmz.sapientia.content.chemistry;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier bioreactor (T-414 / 1.5.0). Cultures organics into nutrient broth. */
public final class SapientiaBioreactor extends MachineEnergyBlock {
    public SapientiaBioreactor(@NotNull Plugin plugin) {
        super(plugin, "bioreactor", Material.SCULK_CATALYST,
                "block.bioreactor.name", EnergyNodeType.CONSUMER, EnergyTier.LOW);
    }
}
