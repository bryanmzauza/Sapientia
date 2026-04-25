package dev.brmz.sapientia.core.overrides;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.overrides.BlockOverride;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.api.overrides.ItemOverride;
import dev.brmz.sapientia.api.overrides.RecipeOverride;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * File-backed {@link ContentOverrides} implementation (T-160/T-161/T-162 / 0.5.0).
 *
 * <p>Reads three YAML files from {@code plugins/Sapientia/overrides/}
 * &mdash; {@code items.yml}, {@code blocks.yml}, {@code recipes.yml}. Each file
 * has a single top-level {@code overrides} map keyed by namespaced id. Invalid
 * entries are logged and skipped; a malformed file never crashes the plugin.
 *
 * <p>The service is thread-safe for read: reloads atomically swap the backing
 * snapshot via {@code volatile}. Writers are serialized via {@code synchronized}.
 */
public final class ContentOverrideService implements ContentOverrides {

    private static final String EXAMPLE_ITEMS = """
            # Sapientia item overrides — reload with `/sapientia reload content`.
            # Removing a key restores the Java default.
            overrides:
            #  sapientia:wrench:
            #    material: IRON_AXE
            #    display_name_key: item.wrench.name
            #    lore_keys:
            #      - item.wrench.lore
            """;
    private static final String EXAMPLE_BLOCKS = """
            # Sapientia block overrides — reload with `/sapientia reload content`.
            overrides:
            #  sapientia:generator:
            #    material: BLAST_FURNACE
            #    display_name_key: block.generator.name
            """;
    private static final String EXAMPLE_RECIPES = """
            # Sapientia recipe overrides — reload with `/sapientia reload content`.
            overrides:
            #  sapientia:recipe_cable:
            #    result_amount: 8
            """;

    private final Logger logger;
    private final Path overridesDir;

    private volatile Map<NamespacedKey, ItemOverride> items = Map.of();
    private volatile Map<NamespacedKey, BlockOverride> blocks = Map.of();
    private volatile Map<NamespacedKey, RecipeOverride> recipes = Map.of();

    public ContentOverrideService(@NotNull Logger logger, @NotNull Path overridesDir) {
        this.logger = logger;
        this.overridesDir = overridesDir;
    }

    /** Eagerly performs the first load and writes example files when missing. */
    public void start() {
        try {
            Files.createDirectories(overridesDir);
            writeExampleIfAbsent("items.yml", EXAMPLE_ITEMS);
            writeExampleIfAbsent("blocks.yml", EXAMPLE_BLOCKS);
            writeExampleIfAbsent("recipes.yml", EXAMPLE_RECIPES);
        } catch (Exception e) {
            logger.warning("Failed to prepare overrides directory: " + e.getMessage());
        }
        reload();
    }

    private void writeExampleIfAbsent(String name, String content) throws Exception {
        Path target = overridesDir.resolve(name);
        if (Files.exists(target)) {
            return;
        }
        try (OutputStream out = Files.newOutputStream(target)) {
            out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @Override
    public @NotNull Optional<ItemOverride> forItem(@NotNull NamespacedKey id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public @NotNull Optional<BlockOverride> forBlock(@NotNull NamespacedKey id) {
        return Optional.ofNullable(blocks.get(id));
    }

    @Override
    public @NotNull Optional<RecipeOverride> forRecipe(@NotNull NamespacedKey id) {
        return Optional.ofNullable(recipes.get(id));
    }

    @Override
    public synchronized @NotNull ReloadReport reload() {
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, ItemOverride> nextItems = new HashMap<>();
        Map<NamespacedKey, BlockOverride> nextBlocks = new HashMap<>();
        Map<NamespacedKey, RecipeOverride> nextRecipes = new HashMap<>();

        loadFile("items.yml", issues, section -> parseItems(section, issues, nextItems));
        loadFile("blocks.yml", issues, section -> parseBlocks(section, issues, nextBlocks));
        loadFile("recipes.yml", issues, section -> parseRecipes(section, issues, nextRecipes));

        this.items = Collections.unmodifiableMap(nextItems);
        this.blocks = Collections.unmodifiableMap(nextBlocks);
        this.recipes = Collections.unmodifiableMap(nextRecipes);

        if (!issues.isEmpty()) {
            for (String issue : issues) {
                logger.warning("[overrides] " + issue);
            }
        }
        return new ReloadReport(nextItems.size(), nextBlocks.size(), nextRecipes.size(), issues);
    }

    private void loadFile(String name, List<String> issues, java.util.function.Consumer<ConfigurationSection> handler) {
        Path file = overridesDir.resolve(name);
        if (!Files.exists(file)) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file.toFile());
        } catch (InvalidConfigurationException e) {
            issues.add(name + ": invalid YAML (" + e.getMessage().replace('\n', ' ') + ")");
            return;
        } catch (Exception e) {
            issues.add(name + ": failed to read (" + e.getMessage() + ")");
            return;
        }
        ConfigurationSection section = yaml.getConfigurationSection("overrides");
        if (section == null) {
            return;
        }
        handler.accept(section);
    }

    // --- parsers -------------------------------------------------------------

    static void parseItems(ConfigurationSection section,
                           List<String> issues,
                           Map<NamespacedKey, ItemOverride> out) {
        for (String key : section.getKeys(false)) {
            NamespacedKey id = NamespacedKey.fromString(key);
            if (id == null) {
                issues.add("items.yml: '" + key + "' is not a valid namespaced key");
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                issues.add("items.yml: '" + key + "' must be a map");
                continue;
            }
            Optional<Material> material = parseMaterial(entry.getString("material"), "items.yml:" + key, issues);
            if (entry.contains("material") && material.isEmpty()) continue; // bad material already reported
            Optional<String> displayNameKey = Optional.ofNullable(entry.getString("display_name_key"))
                    .filter(s -> !s.isBlank());
            Optional<List<String>> loreKeys;
            if (entry.isList("lore_keys")) {
                List<String> raw = entry.getStringList("lore_keys");
                loreKeys = Optional.of(raw);
            } else {
                loreKeys = Optional.empty();
            }
            Optional<Integer> customModelData = Optional.empty();
            if (entry.contains("custom_model_data")) {
                int raw = entry.getInt("custom_model_data", -1);
                if (raw < 0) {
                    issues.add("items.yml: '" + key + "' has invalid custom_model_data="
                            + raw + " (must be >= 0)");
                } else {
                    customModelData = Optional.of(raw);
                }
            }
            out.put(id, new ItemOverride(id, material, displayNameKey, loreKeys, customModelData));
        }
    }

    static void parseBlocks(ConfigurationSection section,
                            List<String> issues,
                            Map<NamespacedKey, BlockOverride> out) {
        for (String key : section.getKeys(false)) {
            NamespacedKey id = NamespacedKey.fromString(key);
            if (id == null) {
                issues.add("blocks.yml: '" + key + "' is not a valid namespaced key");
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                issues.add("blocks.yml: '" + key + "' must be a map");
                continue;
            }
            Optional<Material> material = parseMaterial(entry.getString("material"), "blocks.yml:" + key, issues);
            if (entry.contains("material") && material.isEmpty()) continue;
            Optional<String> displayNameKey = Optional.ofNullable(entry.getString("display_name_key"))
                    .filter(s -> !s.isBlank());
            out.put(id, new BlockOverride(id, material, displayNameKey));
        }
    }

    static void parseRecipes(ConfigurationSection section,
                             List<String> issues,
                             Map<NamespacedKey, RecipeOverride> out) {
        for (String key : section.getKeys(false)) {
            NamespacedKey id = NamespacedKey.fromString(key);
            if (id == null) {
                issues.add("recipes.yml: '" + key + "' is not a valid namespaced key");
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                issues.add("recipes.yml: '" + key + "' must be a map");
                continue;
            }
            Optional<Integer> resultAmount = Optional.empty();
            if (entry.contains("result_amount")) {
                int raw = entry.getInt("result_amount", 0);
                if (raw < 1) {
                    issues.add("recipes.yml: '" + key + "' has invalid result_amount=" + raw + " (must be >= 1)");
                    continue;
                }
                resultAmount = Optional.of(raw);
            }
            out.put(id, new RecipeOverride(id, resultAmount));
        }
    }

    private static Optional<Material> parseMaterial(String raw, String context, List<String> issues) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Material m = Material.matchMaterial(raw);
        if (m == null) {
            issues.add(context + ": unknown material '" + raw + "'");
            return Optional.empty();
        }
        return Optional.of(m);
    }
}
