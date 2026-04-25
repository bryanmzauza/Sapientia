package dev.brmz.sapientia.api;

import java.util.Optional;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.crafting.RecipeRegistry;
import dev.brmz.sapientia.api.energy.EnergyService;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.fluids.FluidService;
import dev.brmz.sapientia.api.guide.GuideService;
import dev.brmz.sapientia.api.guide.UnlockService;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.logic.LogicService;
import dev.brmz.sapientia.api.logistics.ItemService;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level Sapientia service. Additional services (machines, energy, UI) are exposed as
 * sub-services and will be added as the core grows. See docs/api-spec.md.
 */
public interface SapientiaAPI {

    /** Version of the API currently loaded. */
    @NotNull Version version();

    /** Detected platform of a player. Never null; unknown players default to JAVA. */
    @NotNull PlatformType platformOf(@NotNull Player player);

    /**
     * Whether the Floodgate API is available on the server. When false, every player is
     * reported as {@link PlatformType#JAVA}.
     */
    boolean isFloodgateAvailable();

    // --- Content registry (ADR-012) --------------------------------------------------

    /** Registers a Java-defined item with the Sapientia catalog. */
    void registerItem(@NotNull SapientiaItem item);

    /** Registers a Java-defined block with the Sapientia catalog. */
    void registerBlock(@NotNull SapientiaBlock block);

    /** Looks up a registered item by id. */
    @NotNull Optional<SapientiaItem> findItem(@NotNull NamespacedKey id);

    /** Looks up a registered block by id. */
    @NotNull Optional<SapientiaBlock> findBlock(@NotNull NamespacedKey id);

    /**
     * Creates a stack for the registered item with the given id. Returns
     * {@link Optional#empty()} when the id is unknown.
     */
    @NotNull Optional<ItemStack> createStack(@NotNull NamespacedKey id, int amount);

    /** Energy service entry point (T-140 / 0.3.0). */
    @NotNull EnergyService energy();

    /** Item logistics service entry point (T-300 / 1.1.0). */
    @NotNull ItemService logistics();

    /** Fluid logistics service entry point (T-301 / 1.2.0). */
    @NotNull FluidService fluids();

    /** Programmable-logic DAG runtime entry point (T-302 / 1.3.0). */
    @NotNull LogicService logic();

    /** Recipe registry entry point (T-131 / 0.4.0). */
    @NotNull RecipeRegistry recipes();

    /** In-game guide service entry point (T-150 / 0.4.0). */
    @NotNull GuideService guide();

    /** Per-player unlock service entry point (T-151 / 0.4.0). */
    @NotNull UnlockService unlocks();

    /** YAML override service entry point (T-160 / 0.5.0). */
    @NotNull ContentOverrides overrides();

    /**
     * Opens the bundled Machine UI for the given energy node (T-145 / T-202).
     * Routed through {@code UIService.open} so Java players see the chest UI and
     * Bedrock players (when Floodgate is loaded) receive a {@code CustomForm}.
     */
    void openMachineUI(@NotNull Player player, @NotNull EnergyNode node);

    /**
     * Opens a registered UI by its key (T-145). The {@code context} type must
     * match what the descriptor was declared with.
     */
    void openUI(@NotNull Player player, @NotNull NamespacedKey key, @NotNull Object context);
}
