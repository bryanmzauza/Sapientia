package dev.brmz.sapientia.content.gas;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** MV-tier gas compressor (T-426 / 1.6.0). Compresses a fluid into pressurised gas form. */
public final class SapientiaGasCompressor extends MachineEnergyBlock {
    public SapientiaGasCompressor(@NotNull Plugin plugin) {
        super(plugin, "gas_compressor", Material.PISTON,
                "block.gas_compressor.name", EnergyNodeType.CONSUMER, EnergyTier.MID);
    }
}
