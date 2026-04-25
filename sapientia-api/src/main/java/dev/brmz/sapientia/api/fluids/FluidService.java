package dev.brmz.sapientia.api.fluids;

import java.util.Collection;
import java.util.Optional;

import dev.brmz.sapientia.api.energy.EnergyTier;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Public API for the fluid logistics subsystem (T-301 / 1.2.0). Mirrors
 * {@code ItemService} (1.1.0) but tracks per-tank fluid contents.
 */
public interface FluidService {

    /** Registers a {@link FluidType}. Idempotent: re-registering the same id is a no-op. */
    void registerType(@NotNull FluidType type);

    /** Looks up a registered fluid type. */
    @NotNull Optional<FluidType> type(@NotNull NamespacedKey id);

    @NotNull Collection<FluidType> types();

    /** Adds a node at the given block, returning the underlying {@link FluidNode}. */
    @NotNull FluidNode addNode(@NotNull Block block,
                               @NotNull FluidNodeType type,
                               @NotNull EnergyTier tier);

    void removeNode(@NotNull Block block);

    @NotNull Optional<FluidNode> nodeAt(@NotNull Block block);

    @NotNull Optional<FluidNetwork> networkOf(@NotNull FluidNode node);

    @NotNull Collection<FluidNetwork> networks();
}
