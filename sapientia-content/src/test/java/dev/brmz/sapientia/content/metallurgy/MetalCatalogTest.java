package dev.brmz.sapientia.content.metallurgy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetalCatalogTest {

    @Test
    void everyMetalHasEveryForm() {
        for (Metal metal : Metal.values()) {
            assertThat(metal.forms()).hasSize(MetalForm.values().length);
        }
    }

    @Test
    void totalCatalogSizeIs128() {
        // 16 metals and alloys × 8 forms; raw forms were replaced by mineral fragments.
        int total = 0;
        for (Metal metal : Metal.values()) total += metal.forms().size();
        assertThat(total).isEqualTo(128);
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
        assertThat(raw).isEqualTo(10);
        assertThat(alloy).isEqualTo(6);
    }
}
