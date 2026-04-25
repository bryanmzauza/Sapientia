package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier mixer: copper dust + tin dust → bronze dust (T-403 / T-404 / 1.4.0). */
public final class SapientiaMixer extends MachineEnergyBlock {
    public SapientiaMixer(@NotNull Plugin plugin) {
        super(plugin, "mixer", Material.DECORATED_POT,
                "block.mixer.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
