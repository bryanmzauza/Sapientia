package dev.brmz.sapientia.content.metallurgy;

import dev.brmz.sapientia.api.SapientiaAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the full metallurgy item catalog (T-402 / T-403 / 1.4.0):
 * 16 metals and alloys × 8 forms = 128 items registered against the
 * {@link SapientiaAPI} catalog.
 */
public final class MetalCatalog {

    private MetalCatalog() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        for (Metal metal : Metal.values()) {
            for (MetalForm form : metal.forms()) {
                api.registerItem(new MetalItem(plugin, metal, form));
            }
        }
    }

    /** Convenience: id of the (metal, form) pair, in the plugin's namespace. */
    public static @NotNull NamespacedKey idOf(@NotNull Plugin plugin, @NotNull Metal metal, @NotNull MetalForm form) {
        return new NamespacedKey(plugin, metal.idBase() + "_" + form.suffix());
    }
}
