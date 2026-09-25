package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.SapientiaAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the bundled {@link AndroidUpgradeItem} catalogue against the
 * {@link SapientiaAPI} (T-454 / 1.9.0). 16 items spanning AI chip, motor,
 * armour and fuel module across four tiers.
 */
public final class AndroidUpgradeCatalog {

    private AndroidUpgradeCatalog() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        for (AndroidUpgradeItem upgrade : AndroidUpgradeItem.values()) {
            api.registerItem(new AndroidUpgradeContentItem(plugin, upgrade));
        }
    }

    public static @NotNull NamespacedKey idOf(@NotNull Plugin plugin, @NotNull AndroidUpgradeItem upgrade) {
        return new NamespacedKey(plugin, upgrade.idBase());
    }

    public static int total() { return AndroidUpgradeItem.values().length; }
}
