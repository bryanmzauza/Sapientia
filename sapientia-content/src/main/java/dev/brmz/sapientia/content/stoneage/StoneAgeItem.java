package dev.brmz.sapientia.content.stoneage;

import java.util.function.Consumer;

import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.content.CatalogItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * An item of the Stone Age (era 1). Most are plain materials; tools, seeds and
 * food set the matching options when they are built.
 */
public final class StoneAgeItem implements CatalogItem {

    private final NamespacedKey id;
    private final Material material;
    private final GuideCategory category;
    private int benchToolUses;
    private int toolUses;
    private boolean vanillaUse;
    private Consumer<ItemStack> customizer = stack -> { };
    private Consumer<SapientiaItemInteractEvent> onUse = event -> { };

    private StoneAgeItem(Plugin plugin, String id, Material material, GuideCategory category) {
        this.id = new NamespacedKey(plugin, id);
        this.material = material;
        this.category = category;
    }

    static @NotNull StoneAgeItem of(@NotNull Plugin plugin, @NotNull String id, @NotNull Material material,
                                    @NotNull GuideCategory category) {
        return new StoneAgeItem(plugin, id, material, category);
    }

    /** A Sapientia Workbench tool with this many uses. */
    StoneAgeItem benchTool(int uses) {
        this.benchToolUses = uses;
        return this;
    }

    /** A hand tool with this many uses. */
    StoneAgeItem handTool(int uses) {
        this.toolUses = uses;
        return this;
    }

    /** Keeps the vanilla right-click (planting, eating, drinking). */
    StoneAgeItem keepVanillaUse() {
        this.vanillaUse = true;
        return this;
    }

    StoneAgeItem customize(@NotNull Consumer<ItemStack> customizer) {
        this.customizer = customizer;
        return this;
    }

    StoneAgeItem onUse(@NotNull Consumer<SapientiaItemInteractEvent> onUse) {
        this.onUse = onUse;
        return this;
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return material; }
    @Override public @NotNull String displayNameKey() { return "item." + id.getKey() + ".name"; }
    @Override public @NotNull GuideCategory guideCategory() { return category; }
    @Override public int benchToolUses() { return benchToolUses; }
    @Override public int toolUses() { return toolUses; }
    @Override public boolean vanillaUse() { return vanillaUse; }
    @Override public void customizeStack(@NotNull ItemStack stack) { customizer.accept(stack); }
    @Override public void onUse(@NotNull SapientiaItemInteractEvent event) { onUse.accept(event); }
}
