package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Butcher android (T-452 / 1.9.0). Simulated mob loot tables in 1.9.1 (T-455). */
public final class SapientiaAndroidButcher extends AndroidContentBlock {
    public SapientiaAndroidButcher(@NotNull Plugin plugin) {
        super(plugin, AndroidType.BUTCHER.idBase(), Material.RED_NETHER_BRICKS,
                "android.block." + AndroidType.BUTCHER.idBase() + ".name", AndroidType.BUTCHER);
    }
}
