package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Fisherman android (T-452 / 1.9.0). Water-source loot tables in 1.9.1. */
public final class SapientiaAndroidFisherman extends AndroidContentBlock {
    public SapientiaAndroidFisherman(@NotNull Plugin plugin) {
        super(plugin, AndroidType.FISHERMAN.idBase(), Material.PRISMARINE,
                "android.block." + AndroidType.FISHERMAN.idBase() + ".name", AndroidType.FISHERMAN);
    }
}
