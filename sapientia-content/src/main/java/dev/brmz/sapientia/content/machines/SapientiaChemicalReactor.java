package dev.brmz.sapientia.content.machines;

import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergyTier;
import dev.brmz.sapientia.content.energy.MachineEnergyBlock;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** HV-tier chemical reactor (T-423 / 1.6.0). Combines fluid + item inputs. */
public final class SapientiaChemicalReactor extends MachineEnergyBlock {
    public SapientiaChemicalReactor(@NotNull Plugin plugin) {
        super(plugin, "chemical_reactor", Material.CAULDRON,
                "block.chemical_reactor.name", EnergyNodeType.CONSUMER, EnergyTier.HIGH);
    }
}
