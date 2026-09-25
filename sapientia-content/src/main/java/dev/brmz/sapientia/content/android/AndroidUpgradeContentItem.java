package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.item.SapientiaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Single Sapientia item backed by an {@link AndroidUpgradeItem} entry (T-454 / 1.9.0). */
public final class AndroidUpgradeContentItem implements SapientiaItem {

    private final NamespacedKey id;
    private final AndroidUpgradeItem upgrade;

    public AndroidUpgradeContentItem(@NotNull Plugin plugin, @NotNull AndroidUpgradeItem upgrade) {
        this.upgrade = upgrade;
        this.id = new NamespacedKey(plugin, upgrade.idBase());
    }

    @Override public @NotNull NamespacedKey id() { return id; }
    @Override public @NotNull Material baseMaterial() { return upgrade.material(); }
    @Override public @NotNull String displayNameKey() { return "android.upgrade." + upgrade.idBase() + ".name"; }
    @Override public @NotNull GuideCategory guideCategory() { return GuideCategory.MATERIAL; }

    public @NotNull AndroidUpgradeItem upgrade() { return upgrade; }
}
