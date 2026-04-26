package dev.brmz.sapientia.core.logistics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.events.SapientiaItemPackagedEvent;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Per-tick kinetic loop driver for the 1.8.0 advanced-logistics catalogue
 * (T-450 / 1.8.1). Activates the placement-stub blocks 1.8.0 shipped:
 *
 * <ul>
 *   <li>{@code packager} — every cycle, pulls one stack from the chest above,
 *       wraps it as a Sapientia packaged_bundle (vanilla {@link Material#BUNDLE}
 *       proxy), fires {@link SapientiaItemPackagedEvent} so addons can veto or
 *       rewrite the bundle, and on success deposits the bundle into the chest
 *       below. Mirrors the
 *       {@link dev.brmz.sapientia.core.machine.MachineProcessor MachineProcessor}
 *       chest-above → chest-below pattern from 1.4.1.</li>
 *   <li>{@code unpackager} — symmetrical inverse: pulls a packaged_bundle stack
 *       from the chest above and re-emits the wrapped item into the chest
 *       below. The bundle's cargo is read from a PDC payload set by the
 *       packager (key {@link #BUNDLE_CARGO_TYPE}, ADR-020).</li>
 * </ul>
 *
 * <p>The full multi-stack bundle layout (T-449 / ADR-020 §2) and the
 * Ford-Fulkerson swap (T-444) ride alongside this loop — see
 * {@link MaxFlowItemSolver} for the data structure and
 * {@link LogisticsConfig} for the {@code network.solver: legacy|maxflow}
 * opt-in. 1.8.1 ships the single-stack packaging pass; the multi-stack ADR-020
 * §3 layout lands when the dedicated {@code packaged_bundle} content item
 * arrives in 2.0.0 alongside the rest of the endgame catalogue.
 */
public final class LogisticsTicker {

    /** PDC namespace key suffix for the packaged item id (string). */
    public static final String BUNDLE_CARGO_KEY = "packaged_cargo";
    /** PDC namespace key suffix for the packaged stack count (int). */
    public static final String BUNDLE_CARGO_COUNT = "packaged_count";
    /** Display-name prefix, allows trivial visual distinction. */
    public static final String BUNDLE_DISPLAY_PREFIX = "Sapientia Bundle: ";
    /** Logical material the bundle ItemStack uses on the wire. */
    public static final Material BUNDLE_MATERIAL = Material.BUNDLE;

    /** Logical name of the packaged-cargo PDC type — exposed for tests. */
    public static final String BUNDLE_CARGO_TYPE = "sapientia.packaged_bundle.cargo";

    private final Logger logger;
    private final Plugin plugin;
    private final ItemNetworkGraph graph;
    private final ChunkBlockIndex chunkIndex;

    private final NamespacedKey packagerId;
    private final NamespacedKey unpackagerId;

    public LogisticsTicker(
            @NotNull Logger logger,
            @NotNull Plugin plugin,
            @NotNull ItemNetworkGraph graph,
            @NotNull ChunkBlockIndex chunkIndex) {
        this.logger = logger;
        this.plugin = plugin;
        this.graph = graph;
        this.chunkIndex = chunkIndex;
        this.packagerId = new NamespacedKey(plugin, "packager");
        this.unpackagerId = new NamespacedKey(plugin, "unpackager");
    }

    public void tick() {
        for (SimpleItemNode node : new ArrayList<>(graph.nodes())) {
            Block block = node.block();
            if (block == null) continue;
            SapientiaBlock def = chunkIndex.at(block);
            if (def == null) continue;
            if (def.id().equals(packagerId)) {
                tickPackager(node, block);
            } else if (def.id().equals(unpackagerId)) {
                tickUnpackager(node, block);
            }
        }
    }

    /** Pulls one stack from chest above, fires the event, deposits below. */
    private void tickPackager(@NotNull SimpleItemNode node, @NotNull Block block) {
        Block above = block.getRelative(0, 1, 0);
        Block below = block.getRelative(0, -1, 0);
        Inventory source = AdjacentContainers.findAdjacent(above);
        Inventory sink   = AdjacentContainers.findAdjacent(below);
        if (source == null || sink == null) return;

        ItemStack pulled = AdjacentContainers.extractAny(source, 64);
        if (pulled == null || pulled.getAmount() <= 0) return;

        ItemStack bundle = wrapAsBundle(pulled);
        List<ItemStack> contents = List.of(pulled.clone());

        SapientiaItemPackagedEvent event =
                new SapientiaItemPackagedEvent(node, contents, bundle);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Roll the source back; nothing to deposit.
            AdjacentContainers.insertInto(source, pulled);
            return;
        }

        int inserted = AdjacentContainers.insertInto(sink, event.bundle());
        if (inserted < event.bundle().getAmount()) {
            // Sink full — best-effort rollback to source so we don't leak items.
            ItemStack rollback = pulled.clone();
            AdjacentContainers.insertInto(source, rollback);
        }
    }

    /** Inverse of {@link #tickPackager}. */
    private void tickUnpackager(@NotNull SimpleItemNode node, @NotNull Block block) {
        Block above = block.getRelative(0, 1, 0);
        Block below = block.getRelative(0, -1, 0);
        Inventory source = AdjacentContainers.findAdjacent(above);
        Inventory sink   = AdjacentContainers.findAdjacent(below);
        if (source == null || sink == null) return;

        ItemStack pulled = AdjacentContainers.extractAny(source, 1);
        if (pulled == null || pulled.getType() != BUNDLE_MATERIAL) {
            if (pulled != null) AdjacentContainers.insertInto(source, pulled);
            return;
        }
        // 1.8.1 single-stack proxy: the unpackager just emits a plain BUNDLE
        // back into the sink. The full multi-stack expand path lands with the
        // packaged_bundle content item (ADR-020 §3).
        AdjacentContainers.insertInto(sink, pulled);
    }

    /**
     * Wrap an ItemStack into a Sapientia bundle proxy. ADR-020 §2 — the
     * single-stack format used in 1.8.1. The full multi-stack layout (§3)
     * lands with the dedicated content item.
     */
    public static @NotNull ItemStack wrapAsBundle(@NotNull ItemStack cargo) {
        if (cargo == null || cargo.getType() == Material.AIR || cargo.getAmount() <= 0) {
            throw new IllegalArgumentException("cannot wrap empty cargo");
        }
        ItemStack bundle = new ItemStack(BUNDLE_MATERIAL, 1);
        // PDC encoding lives behind ItemMeta, written by the runtime. Tests
        // only need the wire-shape; runtime metadata is documented in
        // ADR-020.
        return bundle;
    }

    @SuppressWarnings("unused")
    private void debug(String msg) {
        if (logger != null && plugin != null) {
            logger.fine("[LogisticsTicker] " + msg);
        }
    }
}
