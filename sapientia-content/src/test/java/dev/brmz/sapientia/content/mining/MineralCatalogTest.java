package dev.brmz.sapientia.content.mining;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.mining.MineralComponent;
import dev.brmz.sapientia.api.mining.SeparationMethod;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.metallurgy.Metal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class MineralCatalogTest {

    private final List<Mineral> minerals = MineralCatalog.minerals();

    @Test
    void has46MineralsWithUniqueIds() {
        assertThat(minerals).hasSize(46);
        Set<String> ids = new HashSet<>();
        for (Mineral mineral : minerals) {
            assertThat(ids.add(mineral.id().getKey())).as("duplicate %s", mineral.id()).isTrue();
        }
    }

    @Test
    void everyElementHasAnEra() {
        Map<String, Era> eras = MineralCatalog.elementEras();
        for (Mineral mineral : minerals) {
            for (MineralComponent component : mineral.composition()) {
                assertThat(eras).as("era of %s in %s", component.element(), mineral.id())
                        .containsKey(component.element());
            }
        }
    }

    @Test
    void everyMineralDropsSomewhere() {
        for (Mineral mineral : minerals) {
            assertThat(mineral.sources()).as("sources of %s", mineral.id()).isNotEmpty();
        }
    }

    @Test
    void mineralsAppearNoEarlierThanEraTwo() {
        // Era 1 has no minerals: the first ones are native metals of the Copper Age.
        for (Mineral mineral : minerals) {
            assertThat(mineral.era().number()).as(mineral.id().toString()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void separationMethodsImproveEraByEra() {
        List<SeparationMethod> methods = MineralCatalog.separationMethods();
        assertThat(methods).hasSize(11);
        for (int i = 1; i < methods.size(); i++) {
            SeparationMethod previous = methods.get(i - 1);
            SeparationMethod current = methods.get(i);
            assertThat(current.era().isAfter(previous.era())).isTrue();
            assertThat(current.primaryYield()).isGreaterThanOrEqualTo(previous.primaryYield());
            assertThat(current.secondaryChance()).isGreaterThanOrEqualTo(previous.secondaryChance());
            assertThat(current.traceChance()).isGreaterThanOrEqualTo(previous.traceChance());
        }
        assertThat(methods.get(methods.size() - 1).isComplete()).isTrue();
    }

    @Test
    void primaryMetalMapsMineralsToBuiltInMetals() {
        Mineral cassiterite = byId("cassiterite");
        Mineral hematite = byId("hematite");
        assertThat(MineralCatalog.primaryMetal(cassiterite)).isEqualTo(Metal.TIN);
        assertThat(MineralCatalog.primaryMetal(hematite)).isNull(); // iron is vanilla
    }

    @Test
    void legacyRawItemsPointAtExistingFragments() {
        Set<String> fragments = new HashSet<>();
        for (Mineral mineral : minerals) fragments.add(mineral.fragmentItem().getKey());
        for (Map.Entry<String, String> entry : LegacyItemIds.replacements().entrySet()) {
            String target = entry.getValue();
            assertThat(fragments.contains(target) || target.equals("silicon_dust"))
                    .as("%s -> %s", entry.getKey(), target).isTrue();
        }
        assertThat(LegacyItemIds.replacements()).hasSize(10);
    }

    private Mineral byId(String id) {
        return minerals.stream().filter(m -> m.id().getKey().equals(id)).findFirst().orElseThrow();
    }
}
