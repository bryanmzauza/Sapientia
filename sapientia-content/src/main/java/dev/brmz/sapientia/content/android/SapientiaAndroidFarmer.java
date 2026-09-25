package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Farmer android (T-452 / 1.9.0). Crop scan + replant in 1.9.1. */
public final class SapientiaAndroidFarmer extends AndroidContentBlock {
    public SapientiaAndroidFarmer(@NotNull Plugin plugin) {
        super(plugin, AndroidType.FARMER.idBase(), Material.HAY_BLOCK,
                "android.block." + AndroidType.FARMER.idBase() + ".name", AndroidType.FARMER);
    }
}
