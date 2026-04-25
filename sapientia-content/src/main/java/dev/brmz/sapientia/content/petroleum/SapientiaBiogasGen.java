package dev.brmz.sapientia.content.petroleum;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** LV-tier biogas generator (T-415 / 1.5.0). Burns nutrient broth into SU. */
public final class SapientiaBiogasGen extends MachineEnergyBlock {
    public SapientiaBiogasGen(@NotNull Plugin plugin) {
        super(plugin, "biogas_gen", Material.CAMPFIRE,
                "block.biogas_gen.name", EnergyNodeType.GENERATOR, EnergyTier.LOW);
    }
}
