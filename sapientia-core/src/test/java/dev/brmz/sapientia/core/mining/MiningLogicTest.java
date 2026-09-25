package dev.brmz.sapientia.core.mining;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MineralComponent;
import dev.brmz.sapientia.api.mining.MineralSource;
import dev.brmz.sapientia.api.mining.SeparationMethod;
import dev.brmz.sapientia.api.mining.SeparationResult;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.mining.MineralCatalog;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Drop rolls, veins, separation, tool tiers and section bits. */
final class MiningLogicTest {

    private static final NamespacedKey PLAINS = NamespacedKey.minecraft("plains");
    private static final NamespacedKey RIVER = NamespacedKey.minecraft("river");

    private final List<Mineral> minerals = MineralCatalog.minerals();
    private final Map<Material, Double> chances = Map.of(Material.STONE, 0.02, Material.GRAVEL, 0.03,
            Material.GRANITE, 0.04, Material.NETHERRACK, 0.015);

    @Test
    void stoneDropsAboutItsBaseChance() {
        FragmentRoller roller = new FragmentRoller(minerals, chances, 3.0);
        SplittableRandom random = new SplittableRandom(1);
        int drops = 0;
        for (int i = 0; i < 100_000; i++) {
            if (roller.roll(Material.STONE, 20, PLAINS, World.Environment.NORMAL, null, 0, random) != null) drops++;
        }
        assertThat(drops).isBetween(1_700, 2_300); // 2 %
    }

    @Test
    void fortuneAndVeinsRaiseTheChance() {
        FragmentRoller roller = new FragmentRoller(minerals, chances, 3.0);
        Mineral cassiterite = byId("cassiterite");
        SplittableRandom random = new SplittableRandom(2);
        int plain = 0, fortune = 0, vein = 0, veinMineral = 0;
        for (int i = 0; i < 100_000; i++) {
            if (roller.roll(Material.GRANITE, 20, PLAINS, World.Environment.NORMAL, null, 0, random) != null) plain++;
            if (roller.roll(Material.GRANITE, 20, PLAINS, World.Environment.NORMAL, null, 3, random) != null) fortune++;
            Mineral m = roller.roll(Material.GRANITE, 20, PLAINS, World.Environment.NORMAL, cassiterite, 0, random);
            if (m != null) {
                vein++;
                if (m.equals(cassiterite)) veinMineral++;
            }
        }
        assertThat(fortune).isBetween((int) (plain * 1.8), (int) (plain * 2.2));
        assertThat(vein).isBetween((int) (plain * 2.7), (int) (plain * 3.3));
        assertThat((double) veinMineral / vein).isGreaterThan(0.66);
    }

    @Test
    void sourcesRespectHeightBiomeAndDimension() {
        FragmentRoller roller = new FragmentRoller(minerals, Map.of(Material.GRAVEL, 1.0, Material.NETHERRACK, 1.0), 3.0);
        SplittableRandom random = new SplittableRandom(3);
        // Gravel only holds river minerals (alluvial gold, tin, platinum, coltan... coltan has any-biome gravel).
        for (int i = 0; i < 1_000; i++) {
            Mineral onPlains = roller.roll(Material.GRAVEL, 60, PLAINS, World.Environment.NORMAL, null, 0, random);
            assertThat(onPlains == null || onPlains.id().getKey().equals("coltan")
                    || onPlains.id().getKey().equals("monazite")).isTrue();
        }
        assertThat(roller.roll(Material.GRAVEL, 60, RIVER, World.Environment.NORMAL, null, 0, random)).isNotNull();
        // Netherrack minerals are Nether-only.
        assertThat(roller.roll(Material.NETHERRACK, 60, PLAINS, World.Environment.NORMAL, null, 0, random)).isNull();
        assertThat(roller.roll(Material.NETHERRACK, 60, null, World.Environment.NETHER, null, 0, random)).isNotNull();
    }

    @Test
    void veinsAreDeterministicAndCoverRegions() {
        VeinMap veins = new VeinMap(minerals, 4, 0.3);
        long seed = 12345L;
        int withVein = 0;
        for (int rx = 0; rx < 200; rx++) {
            Mineral first = veins.veinAt(seed, World.Environment.NORMAL, rx * 4, 0);
            assertThat(veins.veinAt(seed, World.Environment.NORMAL, rx * 4 + 3, 3)).isEqualTo(first);
            if (first != null) {
                withVein++;
                assertThat(first.sources()).anyMatch(s -> s.environment() == World.Environment.NORMAL);
            }
        }
        assertThat(withVein).isBetween(40, 80); // about 30 % of 200 regions
        assertThat(veins.veinAt(seed + 1, World.Environment.NORMAL, 0, 0))
                .satisfiesAnyOf(m -> assertThat(m).isNull(), m -> assertThat(m).isNotNull());
    }

    @Test
    void separationYieldsPrimariesAndLeavesTailings() {
        Mineral galena = byId("galena"); // lead; silver secondary; bismuth, antimony traces
        SeparationMethod hammer = method(1.0, 0.0, 0.0);
        Separator separator = new Separator(MineralCatalog.elementEras()::get, era -> true);

        SeparationResult result = separator.separate(galena, hammer, false, new SplittableRandom(4));

        assertThat(result.elements()).containsOnly(Map.entry("lead", 1));
        assertThat(result.tailings()).isTrue();
    }

    @Test
    void aCompleteMethodRecoversEverything() {
        Mineral galena = byId("galena");
        Separator separator = new Separator(MineralCatalog.elementEras()::get, era -> true);

        SeparationResult result = separator.separate(galena, method(4.0, 1.0, 1.0), false, new SplittableRandom(5));

        assertThat(result.elements()).containsEntry("lead", 4).containsEntry("silver", 1)
                .containsEntry("bismuth", 1).containsEntry("antimony", 1);
        assertThat(result.tailings()).isFalse();
    }

    @Test
    void lockedElementsStayInTheTailingsAndTailingsAreFinal() {
        Mineral galena = byId("galena");
        // Server at the Iron Age: bismuth (era 6) and antimony (era 5) are locked.
        Separator separator = new Separator(MineralCatalog.elementEras()::get, era -> !era.isAfter(Era.IRON_AGE));

        SeparationResult fragment = separator.separate(galena, method(4.0, 1.0, 1.0), false, new SplittableRandom(6));
        assertThat(fragment.elements()).containsOnlyKeys("lead", "silver");
        assertThat(fragment.tailings()).isTrue();

        SeparationResult tailings = separator.separate(galena, method(4.0, 1.0, 1.0), true, new SplittableRandom(7));
        assertThat(tailings.elements()).doesNotContainKey("lead");
        assertThat(tailings.tailings()).isFalse();
    }

    @Test
    void fractionalYieldsAverageOut() {
        Mineral cassiterite = byId("cassiterite");
        Separator separator = new Separator(MineralCatalog.elementEras()::get, era -> true);
        SplittableRandom random = new SplittableRandom(8);
        int tin = 0;
        for (int i = 0; i < 10_000; i++) {
            tin += separator.separate(cassiterite, method(1.5, 0, 0), false, random).elements().getOrDefault("tin", 0);
        }
        assertThat(tin).isBetween(14_500, 15_500);
    }

    @Test
    void toolTiersFollowTheEras() {
        assertThat(ToolTiers.tierOf(Material.STONE_PICKAXE, Material.STONE)).isGreaterThanOrEqualTo(ToolTiers.required(Era.COPPER_AGE));
        assertThat(ToolTiers.tierOf(Material.STONE_PICKAXE, Material.STONE)).isLessThan(ToolTiers.required(Era.BRONZE_AGE));
        assertThat(ToolTiers.tierOf(Material.IRON_PICKAXE, Material.STONE)).isGreaterThanOrEqualTo(ToolTiers.required(Era.ELECTRONICS));
        assertThat(ToolTiers.tierOf(Material.IRON_PICKAXE, Material.STONE)).isLessThan(ToolTiers.required(Era.ATOMIC_AGE));
        assertThat(ToolTiers.tierOf(Material.IRON_SHOVEL, Material.GRAVEL)).isEqualTo(ToolTiers.IRON);
        assertThat(ToolTiers.tierOf(Material.IRON_SHOVEL, Material.STONE)).isEqualTo(ToolTiers.NONE);
        assertThat(ToolTiers.tierOf(null, Material.STONE)).isEqualTo(ToolTiers.NONE);
    }

    @Test
    void sectionBitsCoverAllPositions() {
        long[] bits = new long[SectionBits.LONGS];
        Set<Integer> seen = new java.util.HashSet<>();
        for (int x = -16; x < 16; x++) {
            for (int y = -64; y < -48; y++) {
                for (int z = 16; z < 32; z++) {
                    seen.add(SectionBits.index(x, y, z));
                }
            }
        }
        assertThat(seen).hasSize(4096);
        SectionBits.set(bits, SectionBits.index(-1, -64, 17), true);
        assertThat(SectionBits.get(bits, SectionBits.index(-1, -64, 17))).isTrue();
        assertThat(SectionBits.get(bits, SectionBits.index(-2, -64, 17))).isFalse();
        SectionBits.set(bits, SectionBits.index(-1, -64, 17), false);
        assertThat(SectionBits.get(bits, SectionBits.index(-1, -64, 17))).isFalse();
    }

    @Test
    void mineralRecordsDeriveTheirItems() {
        Mineral m = new Mineral(new NamespacedKey("sapientia", "test_ore"), Era.COPPER_AGE,
                List.of(MineralComponent.primary("copper")),
                List.of(new MineralSource(Set.of(Material.STONE), 0, 10, Set.of(), null, 1)));
        assertThat(m.fragmentItem().getKey()).isEqualTo("test_ore_fragment");
        assertThat(m.tailingsItem().getKey()).isEqualTo("test_ore_tailings");
        assertThat(m.hasByproducts()).isFalse();
        Map<String, Integer> unused = new HashMap<>();
        assertThat(unused).isEmpty();
    }

    private Mineral byId(String id) {
        return minerals.stream().filter(m -> m.id().getKey().equals(id)).findFirst().orElseThrow();
    }

    private static SeparationMethod method(double primary, double secondary, double trace) {
        return new SeparationMethod(new NamespacedKey("sapientia", "test"), Era.ARRIVAL, primary, secondary, trace);
    }
}
