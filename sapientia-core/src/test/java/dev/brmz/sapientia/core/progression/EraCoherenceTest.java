package dev.brmz.sapientia.core.progression;

import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class EraCoherenceTest {

    private static final NamespacedKey COPPER = new NamespacedKey("sapientia", "copper_dust");
    private static final NamespacedKey NICKEL = new NamespacedKey("sapientia", "nickel_dust");
    private static final NamespacedKey ELECTRUM = new NamespacedKey("sapientia", "electrum_dust");
    private static final NamespacedKey BRONZE = new NamespacedKey("sapientia", "bronze_dust");

    private final Map<NamespacedKey, Era> eras = Map.of(
            COPPER, Era.COPPER_AGE, NICKEL, Era.INDUSTRIAL_REVOLUTION,
            ELECTRUM, Era.COPPER_AGE, BRONZE, Era.BRONZE_AGE);

    @Test
    void anIngredientFromALaterEraIsAViolation() {
        List<EraCoherence.Violation> violations = EraCoherence.check(List.of(
                new EraCoherence.Recipe("electrum", ELECTRUM, List.of(COPPER, NICKEL)),
                new EraCoherence.Recipe("bronze", BRONZE, List.of(COPPER))), eras::get);

        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.recipe()).isEqualTo("electrum");
            assertThat(v.ingredient()).isEqualTo(NICKEL);
            assertThat(v.ingredientEra()).isEqualTo(Era.INDUSTRIAL_REVOLUTION);
        });
    }
}
