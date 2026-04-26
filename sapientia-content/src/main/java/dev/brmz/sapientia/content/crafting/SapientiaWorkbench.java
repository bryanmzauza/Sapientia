package dev.brmz.sapientia.content.crafting;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaBlockInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * The Sapientia workbench (T-130 / 0.4.0). Right-clicking opens the 3&times;3
 * crafting window handled by {@code WorkbenchListener} in sapientia-core.
 *
 * <p>Opening the UI is routed through {@link Sapientia#get()} so this content
 * module does not need a compile-time dependency on sapientia-core.
 */
public final class SapientiaWorkbench implements SapientiaBlock {

    private final NamespacedKey id;

    public SapientiaWorkbench(@NotNull Plugin plugin) {
        this.id = new NamespacedKey(plugin, "workbench");
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return Material.CRAFTING_TABLE; }
    @Override public @NotNull String displayNameKey() { return "block.workbench.name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MACHINE; }

    @Override
    public void onInteract(@NotNull SapientiaBlockInteractEvent event) {
        Sapientia.get().recipes().openWorkbench(event.player());
    }
}
