package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidType;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Slayer android (T-452 / 1.9.0). Simulated combat loot tables in 1.9.1.
 *
 * <p>The melee policy (target real mobs in a fixed radius vs. purely
 * simulated loot) is captured in ADR-021 (decision-log). 1.9.0 ships the
 * placement + assignment surface only.
 */
public final class SapientiaAndroidSlayer extends AndroidContentBlock {
    public SapientiaAndroidSlayer(@NotNull Plugin plugin) {
        super(plugin, AndroidType.SLAYER.idBase(), Material.NETHERITE_BLOCK,
                "android.block." + AndroidType.SLAYER.idBase() + ".name", AndroidType.SLAYER);
    }
}
