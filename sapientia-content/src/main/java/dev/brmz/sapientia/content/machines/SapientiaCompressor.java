package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier compressor: 9× dust → block, plate → wire (T-404 / 1.4.0). */
public final class SapientiaCompressor extends MachineEnergyBlock {
    public SapientiaCompressor(@NotNull Plugin plugin) {
        super(plugin, "compressor", Material.PISTON,
                "block.compressor.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
