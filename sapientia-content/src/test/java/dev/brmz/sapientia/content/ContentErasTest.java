package dev.brmz.sapientia.content;

import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.metallurgy.Metal;
import dev.brmz.sapientia.content.metallurgy.MetalForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ContentErasTest {

    @Test
    void everyMetalFormHasAnEra() {
        for (Metal metal : Metal.values()) {
            for (MetalForm form : metal.forms()) {
                assertThat(ContentEras.find(metal.idBase() + "_" + form.suffix())).isNotNull();
            }
        }
    }

    @Test
    void formsNeedTheirTools() {
        assertThat(ContentEras.find("copper_ingot")).isEqualTo(Era.COPPER_AGE);
        assertThat(ContentEras.find("copper_gear")).isEqualTo(Era.BRONZE_AGE);
        assertThat(ContentEras.find("copper_screw")).isEqualTo(Era.RENAISSANCE);
        assertThat(ContentEras.find("copper_wire")).isEqualTo(Era.ELECTRICITY);
        assertThat(ContentEras.find("titanium_wire")).isEqualTo(Era.SPACE_AGE); // metal later than the form
    }

    @Test
    void entryContentIsAlwaysAvailable() {
        assertThat(ContentEras.find("guide")).isEqualTo(Era.ARRIVAL);
        assertThat(ContentEras.find("workbench")).isEqualTo(Era.ARRIVAL);
    }
}
