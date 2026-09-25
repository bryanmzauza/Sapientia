package dev.brmz.sapientia.content.mining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MineralComponent;
import dev.brmz.sapientia.api.mining.MineralSource;
import dev.brmz.sapientia.api.mining.MiningService;
import dev.brmz.sapientia.api.mining.SeparationMethod;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.metallurgy.Metal;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.brmz.sapientia.api.mining.MineralComponent.primary;
import static dev.brmz.sapientia.api.mining.MineralComponent.secondary;
import static dev.brmz.sapientia.api.mining.MineralComponent.trace;

/**
 * The 46 built-in minerals (38 metallic, 8 non-metallic), the era of each
 * element and the separation methods, from {@code docs/jogabilidade.md}
 * sections 4.4, 4.5, 4.7 and 5.1.
 */
public final class MineralCatalog {

    private static final String NS = "sapientia";
    private static final int ANY_LOW = -64;
    private static final int ANY_HIGH = 320;

    private static final Set<Material> TERRACOTTA = Set.of(Material.TERRACOTTA, Material.WHITE_TERRACOTTA,
            Material.ORANGE_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.BROWN_TERRACOTTA,
            Material.RED_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA);
    private static final Set<NamespacedKey> RIVERS = biomes("river", "frozen_river");
    private static final Set<NamespacedKey> BEACHES = biomes("beach", "snowy_beach");
    private static final Set<NamespacedKey> DESERTS = biomes("desert", "badlands", "eroded_badlands", "wooded_badlands");

    private MineralCatalog() {}

    /** Registers elements, minerals with their fragment and tailings items, and separation methods. */
    public static void registerAll(@NotNull SapientiaAPI api) {
        MiningService mining = api.mining();
        elementEras().forEach(mining::registerElement);
        for (Mineral mineral : minerals()) {
            mining.registerMineral(mineral);
            api.registerItem(new MineralItem(mineral, MineralItem.Kind.FRAGMENT));
            api.registerItem(new MineralItem(mineral, MineralItem.Kind.TAILINGS));
        }
        separationMethods().forEach(mining::registerSeparationMethod);
    }

    /** Every built-in mineral. */
    public static @NotNull List<Mineral> minerals() {
        List<Mineral> out = new ArrayList<>();
        // Metallic minerals (section 4.4).
        out.add(mineral("native_copper", Era.COPPER_AGE, List.of(primary("copper"), trace("silver")),
                source(set(Material.STONE, Material.ANDESITE), 0, 80, 12)));
        out.add(mineral("malachite", Era.COPPER_AGE, List.of(primary("copper"), trace("zinc")),
                source(set(Material.CALCITE, Material.STONE), ANY_LOW, 64, 8)));
        out.add(mineral("native_gold", Era.COPPER_AGE, List.of(primary("gold"), secondary("silver")),
                river(set(Material.GRAVEL), 10), source(set(Material.STONE), -20, 40, 4)));
        out.add(mineral("native_silver", Era.COPPER_AGE, List.of(primary("silver"), secondary("copper")),
                source(set(Material.DIORITE), 0, 40, 10)));
        out.add(mineral("chalcopyrite", Era.BRONZE_AGE,
                List.of(primary("copper"), secondary("iron"), trace("gold"), trace("selenium"), trace("tellurium")),
                source(set(Material.DIORITE, Material.STONE), -20, 50, 10)));
        out.add(mineral("cassiterite", Era.BRONZE_AGE, List.of(primary("tin"), trace("niobium"), trace("tantalum")),
                source(set(Material.GRANITE), 0, 60, 12), river(set(Material.GRAVEL), 8)));
        out.add(mineral("arsenopyrite", Era.BRONZE_AGE, List.of(primary("arsenic"), secondary("iron"), trace("gold")),
                source(set(Material.DEEPSLATE, Material.STONE), -30, 30, 6)));
        out.add(mineral("hematite", Era.IRON_AGE, List.of(primary("iron"), trace("manganese")),
                source(union(set(Material.STONE), TERRACOTTA), -10, 80, 12)));
        out.add(mineral("magnetite", Era.IRON_AGE,
                List.of(primary("iron"), secondary("titanium"), trace("vanadium")),
                source(set(Material.STONE, Material.DIORITE), -40, 40, 10)));
        out.add(mineral("galena", Era.IRON_AGE,
                List.of(primary("lead"), secondary("silver"), trace("bismuth"), trace("antimony")),
                source(set(Material.STONE, Material.CALCITE), -20, 40, 10)));
        out.add(mineral("cinnabar", Era.IRON_AGE, List.of(primary("mercury")),
                nether(set(Material.NETHERRACK, Material.BASALT, Material.BLACKSTONE), 10)));
        out.add(mineral("sphalerite", Era.CLASSICAL_ANTIQUITY,
                List.of(primary("zinc"), secondary("cadmium"), secondary("iron"),
                        trace("indium"), trace("germanium"), trace("gallium")),
                source(set(Material.STONE, Material.CALCITE, Material.TUFF), -30, 30, 10)));
        out.add(mineral("stibnite", Era.CLASSICAL_ANTIQUITY, List.of(primary("antimony"), trace("gold")),
                source(set(Material.TUFF), -20, 20, 8), nether(set(Material.NETHERRACK), 5)));
        out.add(mineral("bismuthinite", Era.MIDDLE_AGES, List.of(primary("bismuth")),
                source(set(Material.GRANITE), -10, 30, 6)));
        out.add(mineral("pyrolusite", Era.MIDDLE_AGES, List.of(primary("manganese"), secondary("iron")),
                source(union(set(Material.STONE), TERRACOTTA), 0, 60, 8)));
        out.add(mineral("cobaltite", Era.RENAISSANCE,
                List.of(primary("cobalt"), secondary("arsenic"), trace("nickel")),
                source(set(Material.DEEPSLATE), -50, 0, 8)));
        out.add(mineral("native_platinum", Era.RENAISSANCE,
                List.of(primary("platinum"), secondary("palladium"), secondary("iridium"),
                        trace("osmium"), trace("rhodium"), trace("ruthenium")),
                river(set(Material.GRAVEL), 3), source(set(Material.DEEPSLATE), ANY_LOW, -40, 4)));
        out.add(mineral("pentlandite", Era.INDUSTRIAL_REVOLUTION,
                List.of(primary("nickel"), secondary("iron"), secondary("cobalt"),
                        trace("platinum"), trace("palladium")),
                source(set(Material.DEEPSLATE), -60, 0, 10)));
        out.add(mineral("chromite", Era.INDUSTRIAL_REVOLUTION, List.of(primary("chromium"), secondary("iron")),
                source(set(Material.DEEPSLATE), ANY_LOW, -20, 8),
                nether(set(Material.BASALT, Material.BLACKSTONE), 4)));
        out.add(mineral("wolframite", Era.INDUSTRIAL_REVOLUTION,
                List.of(primary("tungsten"), secondary("iron"), secondary("manganese"), trace("tin")),
                source(set(Material.GRANITE), -30, 20, 6)));
        out.add(mineral("molybdenite", Era.INDUSTRIAL_REVOLUTION, List.of(primary("molybdenum"), trace("rhenium")),
                source(set(Material.GRANITE, Material.DEEPSLATE), -40, 10, 6)));
        out.add(mineral("ilmenite", Era.INDUSTRIAL_REVOLUTION, List.of(primary("titanium"), secondary("iron")),
                beach(set(Material.SAND), 8), source(set(Material.STONE), -20, 20, 5)));
        out.add(mineral("bauxite", Era.ELECTRICITY,
                List.of(primary("aluminum"), secondary("iron"), secondary("titanium"), trace("gallium")),
                source(union(set(Material.CLAY, Material.RED_SAND), TERRACOTTA), 50, ANY_HIGH, 12)));
        out.add(mineral("magnesite", Era.ELECTRICITY, List.of(primary("magnesium")),
                source(set(Material.CALCITE, Material.STONE), 0, 60, 6)));
        out.add(mineral("vanadinite", Era.STEEL_AGE, List.of(primary("vanadium"), secondary("lead")),
                desert(union(set(Material.RED_SAND), TERRACOTTA), 6)));
        out.add(mineral("beryl", Era.OIL_AND_CHEMISTRY, List.of(primary("beryllium"), secondary("aluminum")),
                source(set(Material.GRANITE), -20, 40, 5)));
        out.add(mineral("spodumene", Era.OIL_AND_CHEMISTRY, List.of(primary("lithium"), secondary("aluminum")),
                source(set(Material.GRANITE), -30, 30, 6)));
        out.add(mineral("uraninite", Era.ATOMIC_AGE,
                List.of(primary("uranium"), secondary("thorium"), trace("radium"), trace("lead")),
                source(set(Material.DEEPSLATE, Material.GRANITE), ANY_LOW, -30, 5)));
        out.add(mineral("thorianite", Era.ATOMIC_AGE, List.of(primary("thorium"), secondary("uranium")),
                beach(set(Material.SAND), 4), source(set(Material.GRANITE), ANY_LOW, ANY_HIGH, 3)));
        out.add(mineral("zircon", Era.ATOMIC_AGE,
                List.of(primary("zirconium"), secondary("hafnium"), trace("uranium")),
                source(set(Material.SAND, Material.GRANITE), ANY_LOW, ANY_HIGH, 5)));
        out.add(mineral("rutile", Era.SPACE_AGE, List.of(primary("titanium")),
                source(set(Material.SAND, Material.STONE), ANY_LOW, ANY_HIGH, 5)));
        out.add(mineral("coltan", Era.SPACE_AGE,
                List.of(primary("niobium"), secondary("tantalum"), trace("tin")),
                source(set(Material.GRANITE, Material.GRAVEL), ANY_LOW, ANY_HIGH, 4)));
        out.add(mineral("lepidolite", Era.INFORMATION_AGE,
                List.of(primary("lithium"), secondary("rubidium"), secondary("cesium")),
                source(set(Material.GRANITE), ANY_LOW, ANY_HIGH, 4)));
        out.add(mineral("monazite", Era.INFORMATION_AGE,
                List.of(primary("cerium"), primary("lanthanum"), primary("neodymium"),
                        secondary("thorium"), trace("yttrium")),
                beach(set(Material.SAND), 4), source(set(Material.GRAVEL), ANY_LOW, ANY_HIGH, 2)));
        out.add(mineral("bastnasite", Era.INFORMATION_AGE,
                List.of(primary("cerium"), primary("lanthanum"), primary("neodymium"), trace("europium")),
                source(set(Material.CALCITE, Material.STONE), ANY_LOW, ANY_HIGH, 3)));
        out.add(mineral("xenotime", Era.RENEWABLE_ENERGY,
                List.of(primary("yttrium"), secondary("dysprosium"), secondary("terbium")),
                source(set(Material.GRANITE, Material.SAND), ANY_LOW, ANY_HIGH, 3)));
        out.add(mineral("calaverite", Era.RENEWABLE_ENERGY, List.of(primary("tellurium"), secondary("gold")),
                source(set(Material.TUFF, Material.STONE), ANY_LOW, ANY_HIGH, 3)));
        out.add(mineral("meteorite", Era.ORBITAL_AGE,
                List.of(primary("iron"), primary("nickel"), secondary("cobalt"), trace("iridium"), trace("platinum")),
                new MineralSource(set(Material.END_STONE), ANY_LOW, ANY_HIGH, Set.of(), World.Environment.THE_END, 10)));
        // Non-metallic minerals (section 4.5).
        out.add(mineral("halite", Era.COPPER_AGE, List.of(primary("salt")),
                source(set(Material.STONE, Material.CALCITE), -20, 40, 8)));
        out.add(mineral("native_sulfur", Era.MIDDLE_AGES, List.of(primary("sulfur")),
                nether(set(Material.NETHERRACK, Material.BASALT, Material.BLACKSTONE), 10)));
        out.add(mineral("saltpeter", Era.MIDDLE_AGES, List.of(primary("potassium_nitrate")),
                desert(union(set(Material.RED_SAND), TERRACOTTA), 8)));
        out.add(mineral("phosphorite", Era.INDUSTRIAL_REVOLUTION, List.of(primary("phosphate")),
                source(set(Material.CALCITE, Material.STONE), ANY_LOW, ANY_HIGH, 5)));
        out.add(mineral("graphite", Era.INDUSTRIAL_REVOLUTION, List.of(primary("graphite")),
                source(set(Material.DEEPSLATE), ANY_LOW, ANY_HIGH, 6)));
        out.add(mineral("sylvite", Era.STEEL_AGE, List.of(primary("potassium")),
                source(set(Material.CALCITE, Material.STONE), -30, 20, 5)));
        out.add(mineral("fluorite", Era.ELECTRONICS, List.of(primary("fluorine")),
                source(set(Material.GRANITE), ANY_LOW, ANY_HIGH, 4)));
        out.add(mineral("borax", Era.INFORMATION_AGE, List.of(primary("boron")),
                desert(union(set(Material.RED_SAND), TERRACOTTA), 4)));
        return out;
    }

    /**
     * The built-in metal a mineral is mainly mined for (its first primary
     * element), or {@code null} when that element has no metal items.
     */
    public static @Nullable Metal primaryMetal(@NotNull Mineral mineral) {
        String element = mineral.composition().get(0).element();
        for (Metal metal : Metal.values()) {
            if (!metal.isAlloy() && metal.idBase().equals(element)) return metal;
        }
        return null;
    }

    /** Era from which each element can be separated out of minerals (section 5.1). */
    public static @NotNull Map<String, Era> elementEras() {
        Map<String, Era> eras = new LinkedHashMap<>();
        put(eras, Era.COPPER_AGE, "copper", "gold", "silver", "salt");
        put(eras, Era.BRONZE_AGE, "tin", "arsenic");
        put(eras, Era.IRON_AGE, "iron", "lead", "mercury");
        put(eras, Era.CLASSICAL_ANTIQUITY, "zinc", "antimony");
        put(eras, Era.MIDDLE_AGES, "bismuth", "manganese", "sulfur", "potassium_nitrate");
        put(eras, Era.RENAISSANCE, "cobalt", "platinum");
        put(eras, Era.INDUSTRIAL_REVOLUTION, "nickel", "chromium", "tungsten", "molybdenum", "phosphate", "graphite");
        put(eras, Era.ELECTRICITY, "aluminum", "magnesium", "palladium");
        put(eras, Era.STEEL_AGE, "vanadium", "potassium");
        put(eras, Era.OIL_AND_CHEMISTRY, "beryllium", "lithium", "cadmium");
        put(eras, Era.ELECTRONICS, "silicon", "germanium", "gallium", "indium", "selenium", "fluorine");
        put(eras, Era.ATOMIC_AGE, "uranium", "thorium", "zirconium", "hafnium", "radium");
        put(eras, Era.SPACE_AGE, "titanium", "niobium", "tantalum", "iridium", "osmium");
        put(eras, Era.INFORMATION_AGE, "rubidium", "cesium", "cerium", "lanthanum", "neodymium", "europium",
                "rhenium", "rhodium", "ruthenium", "boron");
        put(eras, Era.RENEWABLE_ENERGY, "yttrium", "dysprosium", "terbium", "tellurium");
        return eras;
    }

    /** Separation methods by era (section 4.7). */
    public static @NotNull List<SeparationMethod> separationMethods() {
        return List.of(
                method("hand_hammer", Era.STONE_AGE, 1.0, 0.0, 0.0),
                method("gold_pan", Era.BRONZE_AGE, 1.0, 0.20, 0.0),
                method("cupellation", Era.IRON_AGE, 1.0, 0.35, 0.0),
                method("stamp_mill", Era.CLASSICAL_ANTIQUITY, 1.5, 0.35, 0.0),
                method("steam_hammer", Era.INDUSTRIAL_REVOLUTION, 1.75, 0.45, 0.0),
                method("macerator", Era.ELECTRICITY, 2.0, 0.50, 0.10),
                method("flotation", Era.STEEL_AGE, 2.5, 0.70, 0.20),
                method("chemical_leaching", Era.OIL_AND_CHEMISTRY, 3.0, 0.85, 0.35),
                method("zone_refining", Era.ELECTRONICS, 3.0, 0.90, 0.50),
                method("solvent_extraction", Era.INFORMATION_AGE, 3.0, 0.95, 0.75),
                method("molecular_separation", Era.NANOTECHNOLOGY, 4.0, 1.0, 1.0));
    }

    private static Mineral mineral(String id, Era era, List<MineralComponent> composition, MineralSource... sources) {
        return new Mineral(new NamespacedKey(NS, id), era, composition, List.of(sources));
    }

    private static SeparationMethod method(String id, Era era, double primary, double secondary, double trace) {
        return new SeparationMethod(new NamespacedKey(NS, id), era, primary, secondary, trace);
    }

    private static MineralSource source(Set<Material> hosts, int minY, int maxY, int weight) {
        return new MineralSource(hosts, minY, maxY, Set.of(), World.Environment.NORMAL, weight);
    }

    private static MineralSource river(Set<Material> hosts, int weight) {
        return new MineralSource(hosts, ANY_LOW, ANY_HIGH, RIVERS, World.Environment.NORMAL, weight);
    }

    private static MineralSource beach(Set<Material> hosts, int weight) {
        return new MineralSource(hosts, ANY_LOW, ANY_HIGH, BEACHES, World.Environment.NORMAL, weight);
    }

    private static MineralSource desert(Set<Material> hosts, int weight) {
        return new MineralSource(hosts, ANY_LOW, ANY_HIGH, DESERTS, World.Environment.NORMAL, weight);
    }

    private static MineralSource nether(Set<Material> hosts, int weight) {
        return new MineralSource(hosts, ANY_LOW, ANY_HIGH, Set.of(), World.Environment.NETHER, weight);
    }

    private static Set<Material> set(Material... materials) {
        return Set.of(materials);
    }

    private static Set<Material> union(Set<Material> a, Set<Material> b) {
        Set<Material> out = new java.util.HashSet<>(a);
        out.addAll(b);
        return out;
    }

    private static Set<NamespacedKey> biomes(String... keys) {
        Set<NamespacedKey> out = new java.util.HashSet<>();
        for (String key : keys) out.add(NamespacedKey.minecraft(key));
        return out;
    }

    private static void put(Map<String, Era> eras, Era era, String... elements) {
        for (String element : elements) {
            if (eras.put(element, era) != null) throw new IllegalStateException("Element twice: " + element);
        }
    }
}
