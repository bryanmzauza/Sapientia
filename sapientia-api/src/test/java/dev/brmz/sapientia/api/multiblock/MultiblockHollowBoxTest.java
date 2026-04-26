package dev.brmz.sapientia.api.multiblock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MultiblockHollowBoxTest {

    @Test
    void rejectsEvenAxisLength() {
        assertThatThrownBy(() ->
                MultiblockShapeValidator.validateHollowBox(null, 5, 5, 6, Material.IRON_BLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zLength");
    }

    @Test
    void rejectsTooSmallAxisLength() {
        assertThatThrownBy(() ->
                MultiblockShapeValidator.validateHollowBox(null, 1, 3, 3, Material.IRON_BLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xLength");
    }

    @Test
    void acceptsRefineryShape5x5x7() {
        // 5×5×7 is the canonical oil-refinery shape. We only validate input
        // checking here; the block-walk runs in-game (Bedrock smoke checklist).
        // We assert that no IllegalArgumentException is thrown for valid lengths.
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () ->
                MultiblockShapeValidator.validateHollowBox(null, 5, 5, 7, Material.IRON_BLOCK));
    }
}
