package dev.brmz.sapientia.core.mining;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MiningService;
import dev.brmz.sapientia.api.mining.SeparationMethod;
import dev.brmz.sapientia.api.mining.SeparationResult;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.api.progression.ProgressionService;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link MiningService}. Registries fill up while content registers;
 * the drop tables and vein map are compiled on first use after a change.
 */
public final class MiningServiceImpl implements MiningService {

    private final MiningConfig config;
    private final PlacedBlockTracker tracker;
    private final ProgressionService progression;
    private final ItemRegistry items;
    private final Map<NamespacedKey, Mineral> minerals = new LinkedHashMap<>();
    private final Map<NamespacedKey, Mineral> byItem = new LinkedHashMap<>();
    private final Map<String, Era> elements = new LinkedHashMap<>();
    private final Map<NamespacedKey, SeparationMethod> methods = new LinkedHashMap<>();
    private final Separator separator;
    private FragmentRoller roller;
    private VeinMap veins;

    public MiningServiceImpl(@NotNull MiningConfig config, @NotNull PlacedBlockTracker tracker,
                             @NotNull ProgressionService progression, @NotNull ItemRegistry items) {
        this.config = config;
        this.tracker = tracker;
        this.progression = progression;
        this.items = items;
        this.separator = new Separator(elements::get, progression::isUnlocked);
    }

    public @NotNull MiningConfig config() {
        return config;
    }

    public @NotNull PlacedBlockTracker tracker() {
        return tracker;
    }

    // --- Registration ------------------------------------------------------------------

    @Override
    public void registerMineral(@NotNull Mineral mineral) {
        if (minerals.putIfAbsent(mineral.id(), mineral) != null) {
            throw new IllegalStateException("Duplicate mineral " + mineral.id());
        }
        byItem.put(mineral.fragmentItem(), mineral);
        byItem.put(mineral.tailingsItem(), mineral);
        roller = null;
        veins = null;
    }

    @Override
    public void registerElement(@NotNull String element, @NotNull Era era) {
        elements.put(element, era);
    }

    @Override
    public void registerSeparationMethod(@NotNull SeparationMethod method) {
        if (methods.putIfAbsent(method.id(), method) != null) {
            throw new IllegalStateException("Duplicate separation method " + method.id());
        }
    }

    @Override
    public @NotNull Collection<Mineral> minerals() {
        return Collections.unmodifiableCollection(minerals.values());
    }

    @Override
    public @NotNull Optional<Mineral> mineral(@NotNull NamespacedKey id) {
        return Optional.ofNullable(minerals.get(id));
    }

    @Override
    public @NotNull Optional<Mineral> mineralOfItem(@NotNull NamespacedKey itemId) {
        return Optional.ofNullable(byItem.get(itemId));
    }

    @Override
    public @NotNull Collection<SeparationMethod> separationMethods() {
        return Collections.unmodifiableCollection(methods.values());
    }

    @Override
    public @NotNull Optional<SeparationMethod> separationMethod(@NotNull NamespacedKey id) {
        return Optional.ofNullable(methods.get(id));
    }

    @Override
    public @Nullable Era elementEra(@NotNull String element) {
        return elements.get(element);
    }

    // --- Terrain and drops -------------------------------------------------------------

    @Override
    public boolean isNatural(@NotNull Block block) {
        return tracker.isNatural(block);
    }

    @Override
    public void markPlaced(@NotNull Block block) {
        tracker.markPlaced(block);
    }

    /** Whether a block type can drop fragments at all (a cheap filter before the natural check). */
    public boolean isHost(@NotNull org.bukkit.Material material) {
        return config.enabled() && roller().hosts().contains(material);
    }

    @Override
    public @NotNull List<ItemStack> rollFragments(@NotNull Block block, @Nullable ItemStack tool) {
        if (!config.enabled() || !roller().hosts().contains(block.getType())) return List.of();
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) return List.of();
        if (!tracker.isNatural(block)) return List.of();
        World world = block.getWorld();
        Mineral vein = veins().veinAt(world.getSeed(), world.getEnvironment(), block.getX() >> 4, block.getZ() >> 4);
        int fortune = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.FORTUNE);
        Mineral mineral = roller().roll(block.getType(), block.getY(), biomeOf(block), world.getEnvironment(),
                vein, fortune, ThreadLocalRandom.current());
        if (mineral == null || !progression.isUnlocked(mineral.era())) return List.of();
        int tier = ToolTiers.tierOf(tool == null ? null : tool.getType(), block.getType());
        if (tier < ToolTiers.required(mineral.era())) return List.of();
        ItemStack fragment = items.createStack(mineral.fragmentItem().toString(), 1);
        return fragment == null ? List.of() : List.of(fragment);
    }

    @Override
    public @NotNull SeparationResult separate(@NotNull Mineral mineral, @NotNull SeparationMethod method,
                                              boolean fromTailings, @NotNull RandomGenerator random) {
        return separator.separate(mineral, method, fromTailings, random);
    }

    @Override
    public @NotNull Optional<Mineral> veinAt(@NotNull World world, int chunkX, int chunkZ) {
        return Optional.ofNullable(veins().veinAt(world.getSeed(), world.getEnvironment(), chunkX, chunkZ));
    }

    private FragmentRoller roller() {
        FragmentRoller r = roller;
        if (r == null) {
            r = new FragmentRoller(minerals.values(), config.hostChance(), config.veinMultiplier());
            roller = r;
        }
        return r;
    }

    private VeinMap veins() {
        VeinMap v = veins;
        if (v == null) {
            v = new VeinMap(minerals.values(), config.veinRegionChunks(), config.veinChance());
            veins = v;
        }
        return v;
    }

    private static @Nullable NamespacedKey biomeOf(Block block) {
        return block.getBiome().getKey();
    }
}
