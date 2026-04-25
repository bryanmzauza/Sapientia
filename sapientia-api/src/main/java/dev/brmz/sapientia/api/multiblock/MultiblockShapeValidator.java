package dev.brmz.sapientia.api.multiblock;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Validates that a 3-dimensional shape of blocks centered at a controller
 * matches a declared casing material (T-405 / T-409 / 1.4.0).
 *
 * <p>This helper is intentionally simple: it walks an axis-aligned bounding box
 * around the controller and asserts every cell (excluding the controller block
 * itself) is one of the allowed casing materials. Hollow shapes are <em>not</em>
 * supported here — for hollow shells (e.g. a 3×3×3 with empty center for fluid)
 * use {@link #validateHollowCube(Block, int, Material...)}.
 *
 * <p>All coordinates are world-space and read off the controller's
 * {@link Block#getWorld()} via {@link Block#getRelative(int, int, int)}.
 */
public final class MultiblockShapeValidator {

    private MultiblockShapeValidator() {}

    /**
     * Validates a solid {@code radius}×{@code radius}×{@code radius} cube around
     * the controller (controller occupies the center cell). The controller block
     * itself is skipped.
     *
     * @param controller   the central block (typically the multiblock controller)
     * @param edgeLength   total edge length in blocks (must be odd, &gt;= 3)
     * @param allowedCasings materials acceptable as casing
     * @return {@code true} iff every non-center cell is one of {@code allowedCasings}
     */
    public static boolean validateSolidCube(
            @NotNull Block controller, int edgeLength, @NotNull Material... allowedCasings) {
        if (edgeLength < 3 || (edgeLength & 1) == 0) {
            throw new IllegalArgumentException("edgeLength must be an odd integer >= 3");
        }
        Set<Material> allowed = toSet(allowedCasings);
        int half = edgeLength / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block b = controller.getRelative(dx, dy, dz);
                    if (!allowed.contains(b.getType())) return false;
                }
            }
        }
        return true;
    }

    /**
     * Validates a hollow shell of casing around the controller: every block on
     * the cube surface must be casing; interior cells (between center and shell)
     * are unchecked.
     */
    public static boolean validateHollowCube(
            @NotNull Block controller, int edgeLength, @NotNull Material... allowedCasings) {
        if (edgeLength < 3 || (edgeLength & 1) == 0) {
            throw new IllegalArgumentException("edgeLength must be an odd integer >= 3");
        }
        Set<Material> allowed = toSet(allowedCasings);
        int half = edgeLength / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    boolean onShell =
                            Math.abs(dx) == half || Math.abs(dy) == half || Math.abs(dz) == half;
                    if (!onShell) continue;
                    Block b = controller.getRelative(dx, dy, dz);
                    if (!allowed.contains(b.getType())) return false;
                }
            }
        }
        return true;
    }

    private static Set<Material> toSet(Material[] mats) {
        Set<Material> out = new HashSet<>();
        for (Material m : mats) out.add(Objects.requireNonNull(m, "material entry"));
        return out;
    }
}
