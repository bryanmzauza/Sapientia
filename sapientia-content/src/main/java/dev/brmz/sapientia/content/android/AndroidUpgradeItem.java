package dev.brmz.sapientia.content.android;

import dev.brmz.sapientia.api.android.AndroidUpgradeKind;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * The 16 android upgrade items shipped in 1.9.0 (T-454): four
 * {@link AndroidUpgradeKind} categories × four tiers.
 *
 * <p>Tier scaling is documented on {@link AndroidUpgradeKind}. Effects are
 * applied by the kinetic loop in 1.9.1; in 1.9.0 the items are craftable
 * via the workbench and {@code AndroidService.setUpgrade} accepts them, but
 * no behaviour change is observable yet.
 */
public enum AndroidUpgradeItem {
    AI_CHIP_T1     (AndroidUpgradeKind.AI_CHIP,     1, Material.LIGHT_BLUE_DYE),
    AI_CHIP_T2     (AndroidUpgradeKind.AI_CHIP,     2, Material.BLUE_DYE),
    AI_CHIP_T3     (AndroidUpgradeKind.AI_CHIP,     3, Material.PURPLE_DYE),
    AI_CHIP_T4     (AndroidUpgradeKind.AI_CHIP,     4, Material.PINK_DYE),

    MOTOR_CHIP_T1  (AndroidUpgradeKind.MOTOR,       1, Material.IRON_HORSE_ARMOR),
    MOTOR_CHIP_T2  (AndroidUpgradeKind.MOTOR,       2, Material.GOLDEN_HORSE_ARMOR),
    MOTOR_CHIP_T3  (AndroidUpgradeKind.MOTOR,       3, Material.DIAMOND_HORSE_ARMOR),
    MOTOR_CHIP_T4  (AndroidUpgradeKind.MOTOR,       4, Material.LEATHER_HORSE_ARMOR),

    ARMOUR_PLATE_T1(AndroidUpgradeKind.ARMOUR,      1, Material.LEATHER_CHESTPLATE),
    ARMOUR_PLATE_T2(AndroidUpgradeKind.ARMOUR,      2, Material.IRON_CHESTPLATE),
    ARMOUR_PLATE_T3(AndroidUpgradeKind.ARMOUR,      3, Material.DIAMOND_CHESTPLATE),
    ARMOUR_PLATE_T4(AndroidUpgradeKind.ARMOUR,      4, Material.NETHERITE_CHESTPLATE),

    FUEL_MODULE_T1 (AndroidUpgradeKind.FUEL_MODULE, 1, Material.COAL),
    FUEL_MODULE_T2 (AndroidUpgradeKind.FUEL_MODULE, 2, Material.CHARCOAL),
    FUEL_MODULE_T3 (AndroidUpgradeKind.FUEL_MODULE, 3, Material.BLAZE_POWDER),
    FUEL_MODULE_T4 (AndroidUpgradeKind.FUEL_MODULE, 4, Material.NETHER_STAR);

    private final AndroidUpgradeKind kind;
    private final int tier;
    private final Material material;

    AndroidUpgradeItem(@NotNull AndroidUpgradeKind kind, int tier, @NotNull Material material) {
        this.kind = kind;
        this.tier = tier;
        this.material = material;
    }

    public @NotNull AndroidUpgradeKind kind()   { return kind; }
    public int                          tier()  { return tier; }
    public @NotNull Material            material() { return material; }

    public @NotNull String idBase() {
        return kind.idBase() + "_t" + tier;
    }
}
