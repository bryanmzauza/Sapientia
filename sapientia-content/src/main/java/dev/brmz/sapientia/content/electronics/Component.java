package dev.brmz.sapientia.content.electronics;

import java.util.Locale;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Electronics component catalogue (T-422 / 1.6.0).
 *
 * <p>Each entry is a tier-tagged manufactured item registered through
 * {@link ComponentCatalog}. The {@code idBase()} maps directly to the
 * registry id (e.g. {@code sapientia:processor_t2}) and to the i18n key
 * {@code component.<id>.name}.
 */
public enum Component {
    // Intermediate.
    SILICON_WAFER ("silicon_wafer", Material.PAPER),

    // Motors.
    MOTOR_T1      ("motor_t1",      Material.IRON_HORSE_ARMOR),
    MOTOR_T2      ("motor_t2",      Material.GOLDEN_HORSE_ARMOR),
    MOTOR_T3      ("motor_t3",      Material.DIAMOND_HORSE_ARMOR),

    // Circuits.
    CIRCUIT_T1    ("circuit_t1",    Material.LIGHT_BLUE_DYE),
    CIRCUIT_T2    ("circuit_t2",    Material.BLUE_DYE),
    CIRCUIT_T3    ("circuit_t3",    Material.PURPLE_DYE),

    // Processors.
    PROCESSOR_T1  ("processor_t1",  Material.HEART_OF_THE_SEA),
    PROCESSOR_T2  ("processor_t2",  Material.NETHER_STAR),
    PROCESSOR_T3  ("processor_t3",  Material.NAUTILUS_SHELL),

    // Coils.
    COIL_T1       ("coil_t1",       Material.STRING),
    COIL_T2       ("coil_t2",       Material.LEAD),
    COIL_T3       ("coil_t3",       Material.IRON_NUGGET),

    // RAM.
    RAM_T2        ("ram_t2",        Material.MUSIC_DISC_CHIRP),
    RAM_T3        ("ram_t3",        Material.MUSIC_DISC_BLOCKS),

    // Storage.
    STORAGE_HDD   ("storage_hdd",   Material.MUSIC_DISC_FAR),
    STORAGE_SSD   ("storage_ssd",   Material.MUSIC_DISC_STAL);

    private final String idBase;
    private final Material material;

    Component(@NotNull String idBase, @NotNull Material material) {
        this.idBase = idBase;
        this.material = material;
    }

    public @NotNull String idBase() { return idBase; }
    public @NotNull Material material() { return material; }
    public @NotNull String displayKeyEn() { return "component." + idBase + ".name"; }
    public @NotNull String lowerName() { return name().toLowerCase(Locale.ROOT); }
}
