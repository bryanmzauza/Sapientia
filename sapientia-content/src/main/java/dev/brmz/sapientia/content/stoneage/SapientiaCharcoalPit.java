package dev.brmz.sapientia.content.stoneage;

import dev.brmz.sapientia.api.events.SapientiaBlockPlaceEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.CatalogBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.Lightable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The charcoal pit (era 1). Logs go in a container above it, charcoal and wood
 * ash come out into a container below; the burn itself is run by the core
 * ({@code CharcoalPitProcessor}). The pit is lit only while burning.
 */
public final class SapientiaCharcoalPit implements CatalogBlock {

    private final NamespacedKey id;

    public SapientiaCharcoalPit(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "charcoal_pit");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.CAMPFIRE; }
    @Override public @NotNull String displayNameKey() { return "block.charcoal_pit.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }
    @Override public int chunkLimit() { return 8; }

    @Override
    public void onPlace(@NotNull SapientiaBlockPlaceEvent event) {
        if (event.block().getBlockData() instanceof Lightable data) {
            data.setLit(false); // lights up when a burn starts
            event.block().setBlockData(data, false);
        }
    }
}
