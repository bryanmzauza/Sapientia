package dev.brmz.sapientia.content;

import java.util.HashMap;
import java.util.Map;

import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.metallurgy.Metal;
import dev.brmz.sapientia.content.metallurgy.MetalForm;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Era of every built-in item and block, following {@code docs/jogabilidade.md}
 * (section 8) and the era documents. Content classes read their era from here
 * through {@link CatalogItem} and {@link CatalogBlock}; minerals declare their
 * own.
 */
public final class ContentEras {

    private static final Map<String, Era> ERAS = new HashMap<>();

    static {
        put(Era.ARRIVAL, "guide", "workbench");
        put(Era.COPPER_AGE, "pedestal");
        put(Era.INDUSTRIAL_REVOLUTION, "boiler", "condenser", "fluid_pipe", "fluid_pump", "fluid_tank", "fluid_drain");
        put(Era.ELECTRICITY, "generator", "cable", "capacitor", "consumer", "console", "wrench", "machine_casing",
                "macerator", "ore_washer", "electric_furnace", "bench_saw",
                "item_cable", "item_producer", "item_consumer", "item_filter",
                "motor_t1", "coil_t1");
        put(Era.STEEL_AGE, "cable_t2", "capacitor_t2", "transformer_lv_mv", "machine_casing_mv",
                "mixer", "compressor", "plate_press", "extractor", "induction_furnace_controller",
                "stainless_steel_casing", "quarry_controller", "prospector",
                "item_buffer", "item_splitter", "filter_chamber", "overflow_module", "comparator_sensor",
                "conveyor_belt", "fluid_valve", "fluid_level_sensor");
        put(Era.OIL_AND_CHEMISTRY, "pumpjack", "oil_refinery_controller", "combustion_gen", "biogas_gen",
                "gas_turbine", "cracker", "fermenter", "still", "bioreactor", "pressurized_pipe", "gas_compressor",
                "drill_rig_controller", "gas_extractor", "packager", "unpackager", "circuit_t1");
        put(Era.ELECTRONICS, "silicon_wafer", "processor_t1", "motor_t2", "circuit_t2", "coil_t2", "processor_t2",
                "ram_t2", "cable_t3", "capacitor_t3", "transformer_mv_hv", "electrolyzer", "rolling_mill",
                "laser_cutter", "chemical_reactor", "liquefier", "phase_separator", "atmospheric_collector");
        put(Era.SPACE_AGE, "rtg", "gps_transmitter", "gps_marker", "gps_handheld_map");
        put(Era.INFORMATION_AGE, "motor_t3", "circuit_t3", "coil_t3", "processor_t3", "ram_t3", "storage_hdd");
        put(Era.ROBOTICS, "storage_ssd",
                "android_farmer", "android_lumberjack", "android_miner", "android_fisherman",
                "android_butcher", "android_builder", "android_slayer", "android_trader");
        for (String kind : new String[] {"ai_chip", "motor_chip", "armour_plate", "fuel_module"}) {
            for (int tier = 1; tier <= 4; tier++) {
                put(Era.ROBOTICS, kind + "_t" + tier);
            }
        }
        put(Era.RENEWABLE_ENERGY, "geothermal_gen", "desalinator_controller");

        for (Metal metal : Metal.values()) {
            for (MetalForm form : metal.forms()) {
                put(metalEra(metal, form), metal.idBase() + "_" + form.suffix());
            }
        }
    }

    private ContentEras() {}

    /** Era of a built-in item or block. */
    public static @NotNull Era of(@NotNull NamespacedKey id) {
        Era era = find(id.getKey());
        if (era == null) {
            throw new IllegalStateException("No era declared for " + id + " in ContentEras");
        }
        return era;
    }

    /** Era of a built-in id (without namespace), or {@code null} if none is declared. */
    public static @Nullable Era find(@NotNull String id) {
        return ERAS.get(id);
    }

    /** Era in which a metal can first be extracted or alloyed. */
    public static @NotNull Era metalEra(@NotNull Metal metal) {
        return switch (metal) {
            case COPPER, SILVER, ELECTRUM -> Era.COPPER_AGE;
            case TIN, BRONZE -> Era.BRONZE_AGE;
            case LEAD -> Era.IRON_AGE;
            case ZINC, BRASS -> Era.CLASSICAL_ANTIQUITY;
            case DAMASCUS_STEEL -> Era.MIDDLE_AGES;
            case NICKEL -> Era.INDUSTRIAL_REVOLUTION;
            case ALUMINUM -> Era.ELECTRICITY;
            case STAINLESS_STEEL, NICHROME -> Era.STEEL_AGE;
            case LITHIUM -> Era.OIL_AND_CHEMISTRY;
            case SILICON -> Era.ELECTRONICS;
            case TITANIUM -> Era.SPACE_AGE;
        };
    }

    /**
     * Era of one form of a metal: the metal's era, or later for forms that need
     * later tools (gears from era 3, screws from the lathe in era 7, wire from
     * the draw plate in era 9).
     */
    public static @NotNull Era metalEra(@NotNull Metal metal, @NotNull MetalForm form) {
        Era base = metalEra(metal);
        Era formEra = switch (form) {
            case GEAR -> Era.BRONZE_AGE;
            case SCREW -> Era.RENAISSANCE;
            case WIRE -> Era.ELECTRICITY;
            default -> Era.ARRIVAL;
        };
        return formEra.isAfter(base) ? formEra : base;
    }

    private static void put(Era era, String... ids) {
        for (String id : ids) {
            if (ERAS.put(id, era) != null) {
                throw new IllegalStateException("Era declared twice for " + id);
            }
        }
    }
}
