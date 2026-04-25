package dev.brmz.sapientia.content.electronics;

import dev.brmz.sapientia.api.SapientiaAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the bundled {@link Component} catalogue against the
 * {@link SapientiaAPI} (T-422 / 1.6.0). 17 manufactured items spanning
 * motor, circuit, processor, coil, RAM and storage families across LV/MV/HV
 * tiers, plus the {@code silicon_wafer} intermediate.
 */
public final class ComponentCatalog {

    private ComponentCatalog() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        for (Component component : Component.values()) {
            api.registerItem(new ComponentItem(plugin, component));
        }
    }

    public static @NotNull NamespacedKey idOf(@NotNull Plugin plugin, @NotNull Component component) {
        return new NamespacedKey(plugin, component.idBase());
    }

    public static int total() { return Component.values().length; }
}
