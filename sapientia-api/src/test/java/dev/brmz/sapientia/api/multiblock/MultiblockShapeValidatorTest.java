package dev.brmz.sapientia.api.multiblock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MultiblockShapeValidatorTest {

    @Test
    void rejectsEvenEdgeLengths() {
        assertThatThrownBy(() ->
                MultiblockShapeValidator.validateSolidCube(null, 4, Material.STONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("odd");
    }

    @Test
    void rejectsTooSmallEdgeLengths() {
        assertThatThrownBy(() ->
                MultiblockShapeValidator.validateSolidCube(null, 1, Material.STONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEvenEdgeLengthOnHollowCube() {
        assertThatThrownBy(() ->
                MultiblockShapeValidator.validateHollowCube(null, 2, Material.STONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /*
     * NOTE: full block-walk validation (3×3×3 with mocked Block.getRelative)
     * lives in the Bedrock smoke checklist + an in-game playtest. Mockito is
     * not on the api test classpath, so we keep these unit tests focused on
     * the input-validation contract.
     */
}
