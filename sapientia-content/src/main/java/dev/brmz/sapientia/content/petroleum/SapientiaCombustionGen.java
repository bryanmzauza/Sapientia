package dev.brmz.sapientia.content.petroleum;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier combustion generator (T-415 / 1.5.0). Burns diesel or gasoline into SU. */
public final class SapientiaCombustionGen extends MachineEnergyBlock {
    public SapientiaCombustionGen(@NotNull Plugin plugin) {
        super(plugin, "combustion_gen", Material.SOUL_CAMPFIRE,
                "block.combustion_gen.name", EnergyNodeType.GENERATOR, EnergyTier.MID);
    }
}
