package dev.brmz.sapientia.api.machine;

import java.util.Optional;

import dev.brmz.sapientia.api.energy.EnergyNode;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block-backed automation device registered with Sapientia. Implementations are
 * supplied by the Core; addons consume this interface. See docs/api-spec.md §2.1.
 */
public interface Machine {

    /** Stable registry id, e.g. {@code "sapientia:electric_furnace"}. */
    @NotNull String id();

    @NotNull MachineCategory category();

    @NotNull Location location();

    @NotNull MachineState state();

    /** Energy node backing this machine, or {@code null} when the machine is unpowered. */
    @Nullable EnergyNode energyNode();

    /** Inventory feeding the machine. May be empty; never {@code null}. */
    @NotNull Inventory inputInventory();

    /** Inventory receiving outputs. May be empty; never {@code null}. */
    @NotNull Inventory outputInventory();

    /** Convenience: {@code state() == MachineState.RUNNING}. */
    default boolean isActive() {
        return state() == MachineState.RUNNING;
    }

    /** Current recipe being processed, if any. */
    @NotNull Optional<RecipeProgress> currentRecipe();
}
