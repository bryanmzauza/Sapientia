package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Trader android (T-452 / 1.9.0). Configurable exchange tables in 1.9.1. */
public final class SapientiaAndroidTrader extends AndroidContentBlock {
    public SapientiaAndroidTrader(@NotNull Plugin plugin) {
        super(plugin, AndroidType.TRADER.idBase(), Material.EMERALD_BLOCK,
                "android.block." + AndroidType.TRADER.idBase() + ".name", AndroidType.TRADER);
    }
}
