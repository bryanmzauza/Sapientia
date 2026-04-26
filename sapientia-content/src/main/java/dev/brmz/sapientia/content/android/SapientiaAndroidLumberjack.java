package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Lumberjack android (T-452 / 1.9.0). Tree fell + replant in 1.9.1. */
public final class SapientiaAndroidLumberjack extends AndroidContentBlock {
    public SapientiaAndroidLumberjack(@NotNull Plugin plugin) {
        super(plugin, AndroidType.LUMBERJACK.idBase(), Material.OAK_LOG,
                "android.block." + AndroidType.LUMBERJACK.idBase() + ".name", AndroidType.LUMBERJACK);
    }
}
