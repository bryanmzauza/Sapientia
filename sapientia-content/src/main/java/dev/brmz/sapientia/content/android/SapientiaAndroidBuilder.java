package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Builder android (T-452 / 1.9.0). Schematic execution in 1.9.1. */
public final class SapientiaAndroidBuilder extends AndroidContentBlock {
    public SapientiaAndroidBuilder(@NotNull Plugin plugin) {
        super(plugin, AndroidType.BUILDER.idBase(), Material.SCAFFOLDING,
                "android.block." + AndroidType.BUILDER.idBase() + ".name", AndroidType.BUILDER);
    }
}
