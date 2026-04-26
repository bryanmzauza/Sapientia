package dev.brmz.sapientia.content.electronics;

import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Single Sapientia item backed by a {@link Component} entry (T-422 / 1.6.0). */
public final class ComponentItem implements SapientiaItem {

    private final NamespacedKey id;
    private final Component component;

    public ComponentItem(@NotNull Plugin plugin, @NotNull Component component) {
        this.component = component;
        this.id = new NamespacedKey(plugin, component.idBase());
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return component.material(); }
    @Override public @NotNull String displayNameKey() { return "component." + component.idBase() + ".name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MATERIAL; }

    public @NotNull Component component() { return component; }
}
