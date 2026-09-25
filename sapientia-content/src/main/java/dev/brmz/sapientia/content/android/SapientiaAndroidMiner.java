package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Miner android (T-452 / 1.9.0). Block scan + virtual mining in 1.9.1. */
public final class SapientiaAndroidMiner extends AndroidContentBlock {
    public SapientiaAndroidMiner(@NotNull Plugin plugin) {
        super(plugin, AndroidType.MINER.idBase(), Material.IRON_BLOCK,
                "android.block." + AndroidType.MINER.idBase() + ".name", AndroidType.MINER);
    }
}
