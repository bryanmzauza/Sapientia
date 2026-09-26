package dev.brmz.sapientia.api.agriculture;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * An item that breaking wild vegetation may drop, such as plant fibre from
 * grass cut with a flint knife. Nothing drops while the era is locked.
 *
 * @param item   Sapientia item dropped
 * @param era    era from which it drops
 * @param source hosts, biomes, chance and required tool
 */
public record WildDrop(@NotNull NamespacedKey item, @NotNull Era era, @NotNull WildSeedSource source) {}
