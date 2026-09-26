package dev.brmz.sapientia.content.stoneage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.agriculture.SapientiaPlant;
import dev.brmz.sapientia.api.agriculture.WildDrop;
import dev.brmz.sapientia.api.agriculture.WildSeedSource;
import dev.brmz.sapientia.api.crafting.RecipeIngredient;
import dev.brmz.sapientia.api.crafting.SmeltingRecipe;
import dev.brmz.sapientia.api.events.SapientiaItemInteractEvent;
import dev.brmz.sapientia.api.guide.GuideCategory;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.crafting.BundledRecipe;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Era 1, the Stone Age: farming, flint tools, the charcoal pit and the clay
 * furnace. See {@code docs/eras/era-01-pedra.md}.
 */
public final class StoneAgeContent {

    static final int SALVE_HEAL = 6;
    static final int SALVE_COOLDOWN_TICKS = 30 * 20;
    static final int COMPOST_RADIUS = 1;

    private StoneAgeContent() {}

    public static void registerAll(@NotNull Plugin plugin, @NotNull SapientiaAPI api) {
        registerItems(plugin, api);
        api.registerBlock(new SapientiaHandQuern(plugin));
        api.registerBlock(new SapientiaCharcoalPit(plugin));
        api.registerBlock(new SapientiaClayFurnace(plugin));
        api.registerBlock(new SapientiaFiberBasket(plugin));
        registerPlants(plugin, api);
        registerRecipes(plugin, api);
    }

    private static void registerItems(Plugin plugin, SapientiaAPI api) {
        NamespacedKey salve = new NamespacedKey(plugin, "healing_salve");
        List<StoneAgeItem> items = List.of(
                StoneAgeItem.of(plugin, "flint_knife", Material.FLINT, GuideCategory.TOOL).handTool(64),
                StoneAgeItem.of(plugin, "stone_hammer", Material.FLINT, GuideCategory.TOOL).benchTool(64),
                StoneAgeItem.of(plugin, "plant_fiber", Material.STRING, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "flax_seeds", Material.WHEAT_SEEDS, GuideCategory.MATERIAL).keepVanillaUse(),
                StoneAgeItem.of(plugin, "flax", Material.WHEAT, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "herb_seeds", Material.BEETROOT_SEEDS, GuideCategory.MATERIAL).keepVanillaUse(),
                StoneAgeItem.of(plugin, "medicinal_herb", Material.BEETROOT, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "flour", Material.SUGAR, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "bread_dough", Material.PAPER, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "flour_bread", Material.BREAD, GuideCategory.MISC).keepVanillaUse()
                        .customize(stack -> stack.setData(DataComponentTypes.FOOD,
                                FoodProperties.food().nutrition(8).saturation(9.0f).build())),
                StoneAgeItem.of(plugin, "herbal_tea", Material.POTION, GuideCategory.MISC).keepVanillaUse()
                        .customize(StoneAgeContent::brewTea),
                StoneAgeItem.of(plugin, "healing_salve", Material.HONEYCOMB, GuideCategory.MISC)
                        .customize(stack -> stack.setData(DataComponentTypes.USE_COOLDOWN,
                                UseCooldown.useCooldown(SALVE_COOLDOWN_TICKS / 20f).cooldownGroup(salve).build()))
                        .onUse(StoneAgeContent::applySalve),
                StoneAgeItem.of(plugin, "compost", Material.BROWN_DYE, GuideCategory.MATERIAL)
                        .onUse(StoneAgeContent::spreadCompost),
                StoneAgeItem.of(plugin, "wood_ash", Material.GRAY_DYE, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "lye_bucket", Material.MILK_BUCKET, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "potash", Material.WHITE_DYE, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "fire_clay", Material.FIREWORK_STAR, GuideCategory.MATERIAL),
                StoneAgeItem.of(plugin, "fire_brick", Material.BRICK, GuideCategory.MATERIAL));
        items.forEach(api::registerItem);
    }

    private static void brewTea(ItemStack stack) {
        stack.editMeta(PotionMeta.class, meta -> {
            meta.setColor(Color.fromRGB(0x8a9a3c));
            meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 0), true);
        });
    }

    /** Restores three hearts, then waits 30 s before the next use. */
    private static void applySalve(SapientiaItemInteractEvent event) {
        Player player = event.player();
        ItemStack hand = handStack(event);
        if (player.hasCooldown(hand)) return;
        double max = player.getAttribute(Attribute.MAX_HEALTH) == null ? 20
                : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (player.getHealth() >= max) return;
        player.setHealth(Math.min(max, player.getHealth() + SALVE_HEAL));
        player.setCooldown(hand, SALVE_COOLDOWN_TICKS);
        player.playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.3, 0.2, 0.3);
        consumeOne(event, hand);
    }

    /** Advances every growing crop around the clicked one by one stage. */
    private static void spreadCompost(SapientiaItemInteractEvent event) {
        Block center = event.clickedBlock();
        if (center == null) return;
        boolean grew = false;
        for (int dx = -COMPOST_RADIUS; dx <= COMPOST_RADIUS; dx++) {
            for (int dz = -COMPOST_RADIUS; dz <= COMPOST_RADIUS; dz++) {
                Block block = center.getRelative(dx, 0, dz);
                if (block.getBlockData() instanceof Ageable crop && crop.getAge() < crop.getMaximumAge()
                        && Tag.CROPS.isTagged(block.getType())) {
                    crop.setAge(crop.getAge() + 1);
                    block.setBlockData(crop);
                    block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                            block.getLocation().add(0.5, 0.5, 0.5), 4, 0.3, 0.2, 0.3);
                    grew = true;
                }
            }
        }
        if (grew) {
            event.player().playSound(center.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 1f);
            consumeOne(event, handStack(event));
        }
    }

    private static ItemStack handStack(SapientiaItemInteractEvent event) {
        return event.hand() == EquipmentSlot.OFF_HAND
                ? event.player().getInventory().getItemInOffHand()
                : event.player().getInventory().getItemInMainHand();
    }

    private static void consumeOne(SapientiaItemInteractEvent event, ItemStack hand) {
        hand.setAmount(hand.getAmount() - 1);
        ItemStack left = hand.getAmount() <= 0 ? null : hand;
        if (event.hand() == EquipmentSlot.OFF_HAND) {
            event.player().getInventory().setItemInOffHand(left);
        } else {
            event.player().getInventory().setItemInMainHand(left);
        }
    }

    private static void registerPlants(Plugin plugin, SapientiaAPI api) {
        NamespacedKey knife = new NamespacedKey(plugin, "flint_knife");
        Set<Material> grass = Set.of(Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN);
        Set<NamespacedKey> plains = Set.of(NamespacedKey.minecraft("plains"),
                NamespacedKey.minecraft("sunflower_plains"), NamespacedKey.minecraft("meadow"));
        Set<NamespacedKey> forests = Set.of(NamespacedKey.minecraft("forest"),
                NamespacedKey.minecraft("flower_forest"), NamespacedKey.minecraft("birch_forest"),
                NamespacedKey.minecraft("old_growth_birch_forest"), NamespacedKey.minecraft("dark_forest"),
                NamespacedKey.minecraft("taiga"), NamespacedKey.minecraft("old_growth_pine_taiga"),
                NamespacedKey.minecraft("old_growth_spruce_taiga"));
        api.plants().register(new SapientiaPlant(new NamespacedKey(plugin, "flax"), Era.STONE_AGE,
                new NamespacedKey(plugin, "flax_seeds"), new NamespacedKey(plugin, "flax"), Material.WHEAT,
                1, 2, 1, 2, List.of(new WildSeedSource(grass, plains, 0.10, knife))));
        api.plants().register(new SapientiaPlant(new NamespacedKey(plugin, "medicinal_herb"), Era.STONE_AGE,
                new NamespacedKey(plugin, "herb_seeds"), new NamespacedKey(plugin, "medicinal_herb"),
                Material.BEETROOTS, 1, 3, 1, 2, List.of(new WildSeedSource(grass, forests, 0.10, knife))));
        api.plants().registerWildDrop(new WildDrop(new NamespacedKey(plugin, "plant_fiber"), Era.STONE_AGE,
                new WildSeedSource(grass, Set.of(), 0.30, knife)));
    }

    private static void registerRecipes(Plugin plugin, SapientiaAPI api) {
        RecipeIngredient e = RecipeIngredient.empty();
        RecipeIngredient fiber = sap(plugin, "plant_fiber");
        RecipeIngredient flour = sap(plugin, "flour");
        RecipeIngredient herb = sap(plugin, "medicinal_herb");
        RecipeIngredient ash = sap(plugin, "wood_ash");
        RecipeIngredient hammer = sap(plugin, "stone_hammer");
        RecipeIngredient fireBrick = sap(plugin, "fire_brick");
        RecipeIngredient stick = RecipeIngredient.of(Material.STICK);
        RecipeIngredient cobble = RecipeIngredient.of(Material.COBBLESTONE);
        RecipeIngredient water = RecipeIngredient.of(Material.WATER_BUCKET);

        // Tools
        workbench(plugin, api, "flint_knife", 1, GuideCategory.TOOL,
                e, RecipeIngredient.of(Material.FLINT), e,
                e, stick, e,
                e, e, e);
        workbench(plugin, api, "stone_hammer", 1, GuideCategory.TOOL,
                cobble, fiber, cobble,
                e, stick, e,
                e, e, e);
        // Plant fibre and farming
        vanillaResult(plugin, api, "fiber_to_string", new ItemStack(Material.STRING), GuideCategory.MATERIAL,
                fiber, fiber, fiber,
                e, e, e,
                e, e, e);
        workbench(plugin, api, "hand_quern", 1, GuideCategory.MACHINE,
                e, stick, e,
                e, RecipeIngredient.of(Material.SMOOTH_STONE), e,
                e, RecipeIngredient.of(Material.SMOOTH_STONE), e);
        workbench(plugin, api, "bread_dough", 3, GuideCategory.MATERIAL,
                flour, flour, flour,
                e, water, e,
                e, e, e);
        workbench(plugin, api, "herbal_tea", 1, GuideCategory.MISC,
                e, e, e,
                herb, RecipeIngredient.of(Material.POTION), herb,
                e, e, e);
        workbench(plugin, api, "healing_salve", 2, GuideCategory.MISC,
                herb, herb, herb,
                e, RecipeIngredient.of(Material.CLAY_BALL), e,
                e, e, e);
        List<Material> leaves = new ArrayList<>(Tag.LEAVES.getValues());
        leaves.sort(java.util.Comparator.comparing(Material::name));
        for (Material leaf : leaves) {
            RecipeIngredient l = RecipeIngredient.of(leaf);
            register(api, new NamespacedKey(plugin, "recipe_compost_" + leaf.name().toLowerCase(java.util.Locale.ROOT)),
                    stack(plugin, api, "compost", 1), GuideCategory.MATERIAL,
                    e, l, e,
                    l, RecipeIngredient.of(Material.DIRT), l,
                    e, l, e);
        }
        // Wood, charcoal and chemicals
        RecipeIngredient dirt = RecipeIngredient.of(Material.DIRT);
        workbench(plugin, api, "charcoal_pit", 1, GuideCategory.MACHINE,
                dirt, cobble, dirt,
                cobble, RecipeIngredient.of(Material.CAMPFIRE), cobble,
                dirt, cobble, dirt);
        workbench(plugin, api, "lye_bucket", 1, GuideCategory.MATERIAL,
                e, ash, e,
                ash, water, ash,
                e, ash, e);
        // Hammer work: the tool above the material
        vanillaResult(plugin, api, "cobblestone_to_gravel", new ItemStack(Material.GRAVEL), GuideCategory.MATERIAL,
                hammer, e, e,
                cobble, e, e,
                e, e, e);
        vanillaResult(plugin, api, "gravel_to_sand", new ItemStack(Material.SAND), GuideCategory.MATERIAL,
                hammer, e, e,
                RecipeIngredient.of(Material.GRAVEL), e, e,
                e, e, e);
        // Fire clay and the clay furnace
        RecipeIngredient clay = RecipeIngredient.of(Material.CLAY_BALL);
        RecipeIngredient gravel = RecipeIngredient.of(Material.GRAVEL);
        workbench(plugin, api, "fire_clay", 8, GuideCategory.MATERIAL,
                clay, gravel, clay,
                gravel, e, gravel,
                clay, gravel, clay);
        workbench(plugin, api, "clay_furnace", 1, GuideCategory.MACHINE,
                fireBrick, fireBrick, fireBrick,
                fireBrick, RecipeIngredient.of(Material.FURNACE), fireBrick,
                fireBrick, fireBrick, fireBrick);
        workbench(plugin, api, "fiber_basket", 1, GuideCategory.LOGISTICS,
                fiber, fiber, fiber,
                fiber, RecipeIngredient.of(Material.CHEST), fiber,
                fiber, fiber, fiber);

        // Vanilla furnace
        smelt(plugin, api, "bread_dough", "flour_bread");
        smelt(plugin, api, "lye_bucket", "potash");
        smelt(plugin, api, "fire_clay", "fire_brick");
    }

    private static RecipeIngredient sap(Plugin plugin, String id) {
        return RecipeIngredient.of(new NamespacedKey(plugin, id));
    }

    private static ItemStack stack(Plugin plugin, SapientiaAPI api, String id, int amount) {
        return api.createStack(new NamespacedKey(plugin, id), amount)
                .orElseThrow(() -> new IllegalStateException("Unknown item " + id));
    }

    private static void workbench(Plugin plugin, SapientiaAPI api, String result, int amount, GuideCategory category,
                                  RecipeIngredient... cells) {
        register(api, new NamespacedKey(plugin, "recipe_" + result), stack(plugin, api, result, amount), category, cells);
    }

    private static void vanillaResult(Plugin plugin, SapientiaAPI api, String id, ItemStack result,
                                      GuideCategory category, RecipeIngredient... cells) {
        register(api, new NamespacedKey(plugin, "recipe_" + id), result, category, cells);
    }

    private static void register(SapientiaAPI api, NamespacedKey id, ItemStack result, GuideCategory category,
                                 RecipeIngredient... cells) {
        api.recipes().register(new BundledRecipe(id, List.of(cells), result, category));
    }

    private static void smelt(Plugin plugin, SapientiaAPI api, String input, String result) {
        api.recipes().registerSmelting(SmeltingRecipe.furnace(new NamespacedKey(plugin, "smelt_" + result),
                new NamespacedKey(plugin, input), new NamespacedKey(plugin, result), 200));
    }
}
