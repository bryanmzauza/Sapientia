package dev.brmz.sapientia.core.mining;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code mining} section of {@code config.yml}.
 *
 * @param enabled         whether natural rock drops mineral fragments
 * @param legacyNatural   whether chunks generated before the plugin count as natural
 * @param hostChance      base fragment chance per host block (0..1)
 * @param veinRegionChunks side of a vein region, in chunks
 * @param veinChance      chance that a region holds a vein
 * @param veinMultiplier  fragment chance multiplier inside a vein
 */
public record MiningConfig(
        boolean enabled,
        boolean legacyNatural,
        @NotNull Map<Material, Double> hostChance,
        int veinRegionChunks,
        double veinChance,
        double veinMultiplier) {

    /** Base chances per host block, from {@code jogabilidade.md} section 4.2 (percent). */
    public static final Map<String, Double> DEFAULT_HOST_CHANCE = Map.ofEntries(
            Map.entry("stone", 2.0), Map.entry("andesite", 2.0), Map.entry("diorite", 3.0),
            Map.entry("granite", 4.0), Map.entry("deepslate", 3.5), Map.entry("tuff", 3.0),
            Map.entry("calcite", 3.0), Map.entry("gravel", 3.0), Map.entry("sand", 1.5),
            Map.entry("red_sand", 2.5), Map.entry("terracotta", 2.5), Map.entry("white_terracotta", 2.5),
            Map.entry("orange_terracotta", 2.5), Map.entry("yellow_terracotta", 2.5),
            Map.entry("brown_terracotta", 2.5), Map.entry("red_terracotta", 2.5),
            Map.entry("light_gray_terracotta", 2.5), Map.entry("clay", 1.0), Map.entry("netherrack", 1.5),
            Map.entry("basalt", 2.5), Map.entry("blackstone", 2.5), Map.entry("end_stone", 1.0));

    public static @NotNull MiningConfig defaults() {
        return new MiningConfig(true, true, parse(DEFAULT_HOST_CHANCE, null), 4, 0.3, 3.0);
    }

    public static @NotNull MiningConfig from(@NotNull FileConfiguration config, @NotNull Logger logger) {
        ConfigurationSection section = config.getConfigurationSection("mining");
        if (section == null) return defaults();
        Map<String, Double> percents = new java.util.HashMap<>(DEFAULT_HOST_CHANCE);
        ConfigurationSection hosts = section.getConfigurationSection("host-chance");
        if (hosts != null) {
            for (String key : hosts.getKeys(false)) {
                percents.put(key.toLowerCase(Locale.ROOT), hosts.getDouble(key));
            }
        }
        String legacy = section.getString("legacy-chunks", "natural");
        return new MiningConfig(
                section.getBoolean("enabled", true),
                !"placed".equalsIgnoreCase(legacy),
                parse(percents, logger),
                Math.max(1, section.getInt("vein.region-chunks", 4)),
                Math.max(0, Math.min(1, section.getDouble("vein.chance", 0.3))),
                Math.max(1, section.getDouble("vein.multiplier", 3.0)));
    }

    private static Map<Material, Double> parse(Map<String, Double> percents, Logger logger) {
        Map<Material, Double> out = new EnumMap<>(Material.class);
        for (Map.Entry<String, Double> entry : percents.entrySet()) {
            Material material = Material.matchMaterial(entry.getKey());
            if (material == null || !material.isBlock()) {
                if (logger != null) logger.warning("mining.host-chance: unknown block " + entry.getKey());
                continue;
            }
            out.put(material, Math.max(0, Math.min(100, entry.getValue())) / 100.0);
        }
        return out;
    }
}
