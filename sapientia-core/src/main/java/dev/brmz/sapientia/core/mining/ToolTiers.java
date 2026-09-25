package dev.brmz.sapientia.core.mining;

import java.util.Set;

import dev.brmz.sapientia.api.progression.Era;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mining tool tiers and the tier each era's minerals need: stone for era 2,
 * copper for era 3, bronze for era 4, iron for eras 5 to 12 and diamond from
 * era 13. Shovels count on soft hosts (gravel, sand, clay).
 */
public final class ToolTiers {

    public static final int NONE = -1;
    public static final int WOOD = 0;
    public static final int STONE = 1;
    public static final int COPPER = 2;
    public static final int BRONZE = 3;
    public static final int IRON = 4;
    public static final int DIAMOND = 5;
    public static final int NETHERITE = 6;

    private static final Set<Material> SOFT_HOSTS = Set.of(Material.GRAVEL, Material.SAND, Material.RED_SAND,
            Material.CLAY);

    private ToolTiers() {}

    /** Tier of a vanilla tool used on {@code host}, or {@link #NONE} if it cannot yield minerals. */
    public static int tierOf(@Nullable Material tool, @NotNull Material host) {
        if (tool == null) return NONE;
        return switch (tool) {
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> WOOD;
            case STONE_PICKAXE -> STONE;
            case COPPER_PICKAXE -> COPPER;
            case IRON_PICKAXE -> IRON;
            case DIAMOND_PICKAXE -> DIAMOND;
            case NETHERITE_PICKAXE -> NETHERITE;
            case WOODEN_SHOVEL, GOLDEN_SHOVEL -> SOFT_HOSTS.contains(host) ? WOOD : NONE;
            case STONE_SHOVEL -> SOFT_HOSTS.contains(host) ? STONE : NONE;
            case COPPER_SHOVEL -> SOFT_HOSTS.contains(host) ? COPPER : NONE;
            case IRON_SHOVEL -> SOFT_HOSTS.contains(host) ? IRON : NONE;
            case DIAMOND_SHOVEL -> SOFT_HOSTS.contains(host) ? DIAMOND : NONE;
            case NETHERITE_SHOVEL -> SOFT_HOSTS.contains(host) ? NETHERITE : NONE;
            default -> NONE;
        };
    }

    /** Tier needed for minerals of {@code era}. */
    public static int required(@NotNull Era era) {
        int n = era.number();
        if (n <= 2) return STONE;
        if (n == 3) return COPPER;
        if (n == 4) return BRONZE;
        if (n <= 12) return IRON;
        return DIAMOND;
    }
}
