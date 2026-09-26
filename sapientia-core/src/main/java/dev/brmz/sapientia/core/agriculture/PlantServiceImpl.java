package dev.brmz.sapientia.core.agriculture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.brmz.sapientia.api.agriculture.PlantService;
import dev.brmz.sapientia.api.agriculture.SapientiaPlant;
import dev.brmz.sapientia.api.agriculture.WildDrop;
import dev.brmz.sapientia.api.agriculture.WildSeedSource;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/** Default {@link PlantService}. */
public final class PlantServiceImpl implements PlantService {

    private final PlantTracker tracker;
    private final Map<NamespacedKey, SapientiaPlant> plants = new LinkedHashMap<>();
    private final Map<NamespacedKey, SapientiaPlant> bySeed = new LinkedHashMap<>();
    private final Set<Material> wildHosts = EnumSet.noneOf(Material.class);
    private final List<WildDrop> wildDrops = new ArrayList<>();

    public PlantServiceImpl(@NotNull PlantTracker tracker) {
        this.tracker = tracker;
    }

    public @NotNull PlantTracker tracker() {
        return tracker;
    }

    @Override
    public void register(@NotNull SapientiaPlant plant) {
        if (plants.putIfAbsent(plant.id(), plant) != null) {
            throw new IllegalStateException("Duplicate plant " + plant.id());
        }
        bySeed.put(plant.seedItem(), plant);
        for (WildSeedSource source : plant.wildSources()) {
            wildHosts.addAll(source.hosts());
        }
    }

    @Override
    public void registerWildDrop(@NotNull WildDrop drop) {
        wildDrops.add(drop);
        wildHosts.addAll(drop.source().hosts());
    }

    /** Items dropped by wild vegetation, in registration order. */
    public @NotNull List<WildDrop> wildDrops() {
        return Collections.unmodifiableList(wildDrops);
    }

    @Override
    public @NotNull Collection<SapientiaPlant> plants() {
        return Collections.unmodifiableCollection(plants.values());
    }

    @Override
    public @NotNull Optional<SapientiaPlant> plant(@NotNull NamespacedKey id) {
        return Optional.ofNullable(plants.get(id));
    }

    @Override
    public @NotNull Optional<SapientiaPlant> bySeed(@NotNull NamespacedKey seedItem) {
        return Optional.ofNullable(bySeed.get(seedItem));
    }

    @Override
    public @NotNull Optional<SapientiaPlant> plantAt(@NotNull Block block) {
        NamespacedKey id = tracker.plantAt(block);
        if (id == null) return Optional.empty();
        SapientiaPlant plant = plants.get(id);
        // A crop replaced by something else (trampled, washed away) no longer counts.
        return plant != null && block.getType() == plant.crop() ? Optional.of(plant) : Optional.empty();
    }

    /** Blocks that can drop wild seeds of some plant. */
    public boolean isWildHost(@NotNull Material material) {
        return wildHosts.contains(material);
    }
}
