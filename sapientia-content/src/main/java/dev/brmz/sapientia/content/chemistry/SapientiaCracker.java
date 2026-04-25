package dev.brmz.sapientia.content.chemistry;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier cracker (T-414 / 1.5.0). Cracks heavy fractions into lighter ones. */
public final class SapientiaCracker extends MachineEnergyBlock {
    public SapientiaCracker(@NotNull Plugin plugin) {
        super(plugin, "cracker", Material.BREWING_STAND,
                "block.cracker.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
