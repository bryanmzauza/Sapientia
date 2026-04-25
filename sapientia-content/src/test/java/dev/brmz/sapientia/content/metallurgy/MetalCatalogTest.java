package dev.brmz.sapientia.content.metallurgy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetalCatalogTest {

    @Test
    void rawMetalsHaveAllNineForms() {
        for (Metal metal : Metal.values()) {
            if (metal.isAlloy()) continue;
            assertThat(metal.forms()).hasSize(MetalForm.values().length);
        }
    }

    @Test
    void alloysSkipRawForm() {
        for (Metal metal : Metal.values()) {
            if (!metal.isAlloy()) continue;
            assertThat(metal.forms()).doesNotContain(MetalForm.RAW);
            assertThat(metal.forms()).hasSize(MetalForm.values().length - 1);
        }
    }

    @Test
    void totalCatalogSizeIs78() {
        // T-402 + T-403: 6 raw × 9 forms + 3 alloys × 8 forms = 54 + 24 = 78.
        int total = 0;
        for (Metal metal : Metal.values()) total += metal.forms().size();
        assertThat(total).isEqualTo(78);
    }

    @Test
    void idBaseIsLowercase() {
        for (Metal metal : Metal.values()) {
            assertThat(metal.idBase()).isEqualTo(metal.name().toLowerCase());
        }
    }

    @Test
    void formSuffixesUsedInIds() {
        for (MetalForm form : MetalForm.values()) {
            assertThat(form.suffix()).isNotEmpty();
            assertThat(form.lowerName()).isEqualTo(form.name().toLowerCase());
        }
    }

    @Test
    void rawAndAlloyMetalCounts() {
        long raw = 0, alloy = 0;
        for (Metal m : Metal.values()) {
            if (m.isAlloy()) alloy++; else raw++;
        }
        assertThat(raw).isEqualTo(6);
        assertThat(alloy).isEqualTo(3);
    }
}
