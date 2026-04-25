package dev.brmz.sapientia.core.overrides;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.overrides.BlockOverride;
import dev.brmz.sapientia.api.overrides.ItemOverride;
import dev.brmz.sapientia.api.overrides.RecipeOverride;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Parser-only coverage for {@link ContentOverrideService} (T-160 / 0.5.0).
 * Exercises the pure {@code parseItems}/{@code parseBlocks}/{@code parseRecipes}
 * helpers without touching the filesystem or Bukkit runtime.
 */
final class ContentOverrideServiceTest {

    @Test
    void parsesValidItemOverride() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                """
                overrides:
                  sapientia:wrench:
                    material: IRON_AXE
                    display_name_key: item.wrench.name.custom
                    lore_keys:
                      - item.wrench.lore.a
                      - item.wrench.lore.b
                """));
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, ItemOverride> out = new HashMap<>();
        ContentOverrideService.parseItems(yaml.getConfigurationSection("overrides"), issues, out);
        assertThat(issues).isEmpty();
        assertThat(out).hasSize(1);
        NamespacedKey id = NamespacedKey.fromString("sapientia:wrench");
        ItemOverride ov = out.get(id);
        assertThat(ov.material()).contains(Material.IRON_AXE);
        assertThat(ov.displayNameKey()).contains("item.wrench.name.custom");
        assertThat(ov.loreKeys()).isPresent();
        assertThat(ov.loreKeys().get()).containsExactly("item.wrench.lore.a", "item.wrench.lore.b");
    }

    @Test
    void reportsUnknownMaterial() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                """
                overrides:
                  sapientia:wrench:
                    material: NOT_A_MATERIAL
                """));
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, ItemOverride> out = new HashMap<>();
        ContentOverrideService.parseItems(yaml.getConfigurationSection("overrides"), issues, out);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("unknown material 'NOT_A_MATERIAL'");
        assertThat(out).isEmpty();
    }

    @Test
    void rejectsInvalidNamespacedKey() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                """
                overrides:
                  "NOT A KEY":
                    material: IRON_AXE
                """));
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, ItemOverride> out = new HashMap<>();
        ContentOverrideService.parseItems(yaml.getConfigurationSection("overrides"), issues, out);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("not a valid namespaced key");
        assertThat(out).isEmpty();
    }

    @Test
    void parsesBlockOverride() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                """
                overrides:
                  sapientia:generator:
                    material: BLAST_FURNACE
                    display_name_key: block.generator.name.custom
                """));
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, BlockOverride> out = new HashMap<>();
        ContentOverrideService.parseBlocks(yaml.getConfigurationSection("overrides"), issues, out);
        assertThat(issues).isEmpty();
        BlockOverride ov = out.get(NamespacedKey.fromString("sapientia:generator"));
        assertThat(ov.material()).contains(Material.BLAST_FURNACE);
        assertThat(ov.displayNameKey()).contains("block.generator.name.custom");
    }

    @Test
    void parsesRecipeResultAmount() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                """
                overrides:
                  sapientia:recipe_cable:
                    result_amount: 8
                """));
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, RecipeOverride> out = new HashMap<>();
        ContentOverrideService.parseRecipes(yaml.getConfigurationSection("overrides"), issues, out);
        assertThat(issues).isEmpty();
        RecipeOverride ov = out.get(NamespacedKey.fromString("sapientia:recipe_cable"));
        assertThat(ov.resultAmount()).contains(8);
    }

    @Test
    void rejectsZeroOrNegativeResultAmount() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new java.io.StringReader(
                """
                overrides:
                  sapientia:recipe_cable:
                    result_amount: 0
                """));
        List<String> issues = new ArrayList<>();
        Map<NamespacedKey, RecipeOverride> out = new HashMap<>();
        ContentOverrideService.parseRecipes(yaml.getConfigurationSection("overrides"), issues, out);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0)).contains("result_amount=0");
        assertThat(out).isEmpty();
    }
}
