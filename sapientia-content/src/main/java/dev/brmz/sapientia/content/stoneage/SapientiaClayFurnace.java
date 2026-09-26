package dev.brmz.sapientia.content.stoneage;

import java.util.Set;

import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.CatalogBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The clay furnace (era 1, gateway to era 2). It stands on a vanilla blast
 * furnace, so its interface, progress and hopper automation are the vanilla
 * ones on Java and Bedrock; it burns only coal and charcoal and smelts the
 * recipes registered for it (crushed ores and alloy mixes of eras 2 to 4).
 */
public final class SapientiaClayFurnace implements CatalogBlock {

    private static final Set<Material> FUELS = Set.of(Material.COAL, Material.CHARCOAL);

    private final NamespacedKey id;

    public SapientiaClayFurnace(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "clay_furnace");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.BLAST_FURNACE; }
    @Override public @NotNull String displayNameKey() { return "block.clay_furnace.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }
    @Override public int chunkLimit() { return 16; }
    @Override public boolean vanillaInteraction() { return true; }
    @Override public @NotNull Set<Material> furnaceFuels() { return FUELS; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        StoneAgeBlocks.nameContainer(event);
    }
}
