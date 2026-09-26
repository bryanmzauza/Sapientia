package dev.brmz.sapientia.api.agriculture;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Sapientia plants grown on vanilla crop blocks, and wild seeds by biome.
 * Plants of locked eras cannot be planted and their wild seeds do not drop.
 */
public interface PlantService {

    void register(@NotNull SapientiaPlant plant);

    /** Registers an item dropped by wild vegetation (see {@link WildDrop}). */
    void registerWildDrop(@NotNull WildDrop drop);

    @NotNull Collection<SapientiaPlant> plants();

    @NotNull Optional<SapientiaPlant> plant(@NotNull NamespacedKey id);

    /** The plant whose seed item has this id. */
    @NotNull Optional<SapientiaPlant> bySeed(@NotNull NamespacedKey seedItem);

    /** The Sapientia plant growing at {@code block}, if any. */
    @NotNull Optional<SapientiaPlant> plantAt(@NotNull Block block);
}
