package dev.brmz.sapientia.content.energy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.energy.EnergyNetwork;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyNodeType;
import dev.brmz.sapientia.api.energy.EnergySpecs;
import dev.brmz.sapientia.api.energy.EnergyTier;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

/**
 * Look-to-inspect loop for the Sapientia Wrench. Every tick interval, scans
 * online players holding the wrench, ray-traces the block at their crosshair,
 * and if it is a registered energy node renders a live readout to either the
 * action bar or a boss bar (configurable).
 *
 * <p>Cost is bounded to one ray-trace + one packet per player per interval,
 * which is negligible even with hundreds of concurrent wrench users.
 */
public final class EnergyInspector {

    /** Display mode resolved from {@code config.yml}. */
    public enum Mode {
        OFF, ACTIONBAR, BOSSBAR;

        static @NotNull Mode parse(String raw) {
            if (raw == null) return ACTIONBAR;
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "off", "disabled", "none" -> OFF;
                case "bossbar", "boss_bar", "boss-bar" -> BOSSBAR;
                default -> ACTIONBAR;
            };
        }
    }

    private static final NamespacedKey ITEM_ID_PDC =
            NamespacedKey.fromString("sapientia:item_id");
    private static final String WRENCH_ID = "sapientia:wrench";
    private static final int BAR_SEGMENTS = 10;

    private final Plugin plugin;
    private final Mode mode;
    private final double range;
    private final long intervalTicks;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private BukkitTask task;

    public EnergyInspector(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.mode = Mode.parse(plugin.getConfig().getString("inspector.display", "actionbar"));
        this.range = Math.max(1.0, plugin.getConfig().getDouble("inspector.range", 6.0));
        this.intervalTicks = Math.max(1L, plugin.getConfig().getLong("inspector.interval-ticks", 10L));
    }

    public void start() {
        if (mode == Mode.OFF) {
            plugin.getLogger().info("Energy inspector disabled via config (inspector.display=off).");
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Map.Entry<UUID, BossBar> e : bossBars.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null) p.hideBossBar(e.getValue());
        }
        bossBars.clear();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isWrench(player.getInventory().getItemInMainHand())) {
                clearFor(player);
                continue;
            }
            RayTraceResult hit = player.rayTraceBlocks(range);
            Block block = hit == null ? null : hit.getHitBlock();
            if (block == null) {
                clearFor(player);
                continue;
            }
            EnergyNode node = Sapientia.get().energy().nodeAt(block).orElse(null);
            if (node == null) {
                clearFor(player);
                continue;
            }
            render(player, node);
        }
    }

    private void render(@NotNull Player player, @NotNull EnergyNode node) {
        EnergyNodeType type = node.type();
        EnergyTier tier = node.tier();
        long current = node.bufferCurrent();
        long max = node.bufferMax();
        EnergyNetwork network = Sapientia.get().energy().networkOf(node).orElse(null);

        switch (mode) {
            case ACTIONBAR -> player.sendActionBar(buildActionBar(type, tier, current, max, network));
            case BOSSBAR -> {
                float fill = max <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) current / (float) max));
                upsertBossBar(player, buildBossBarTitle(type, tier, current, max, network),
                        fill, bossColorFor(type));
            }
            case OFF -> { /* unreachable — task never started */ }
        }
    }

    private void clearFor(@NotNull Player player) {
        if (mode == Mode.BOSSBAR) {
            BossBar bar = bossBars.remove(player.getUniqueId());
            if (bar != null) player.hideBossBar(bar);
        }
        // Action bar clears itself (~3 s) without explicit work.
    }

    private void upsertBossBar(@NotNull Player player, @NotNull Component title,
                               float progress, @NotNull BossBar.Color color) {
        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, color, BossBar.Overlay.PROGRESS);
            bossBars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
            bar.progress(progress);
            bar.color(color);
        }
    }

    // ---------------------------------------------------------------- rendering

    private static final TextColor SEPARATOR_COLOR = NamedTextColor.DARK_GRAY;
    private static final TextColor LABEL_COLOR = NamedTextColor.GRAY;
    private static final TextColor VALUE_COLOR = NamedTextColor.WHITE;
    private static final String SEPARATOR = "  \u2503  "; // box-drawing vertical line, padded

    private static @NotNull Component buildActionBar(
            @NotNull EnergyNodeType type, @NotNull EnergyTier tier,
            long current, long max, EnergyNetwork network) {
        TextColor accent = accentFor(type);

        // [icon TYPE · TIER]   ▰▰▰▰▱▱▱▱▱▱  42%   1 234 / 4 000 E   ▲ +32 E/t   ✦ Net 5n  12%
        Component header = Component.text()
                .append(Component.text("\u00bb ", SEPARATOR_COLOR))
                .append(Component.text(iconFor(type) + " ", accent))
                .append(Component.text(longType(type), accent).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" \u00b7 ", SEPARATOR_COLOR))
                .append(Component.text(tier.name(), tierColorFor(tier)))
                .build();

        Component bar = renderBar(current, max, accent);
        long pct = max <= 0 ? 0 : Math.round(((double) current / max) * 100);
        Component pctText = Component.text(pct + "%", percentColor(pct))
                .decoration(TextDecoration.BOLD, true);

        Component buffer = Component.text()
                .append(Component.text(formatNumber(current), VALUE_COLOR))
                .append(Component.text(" / ", SEPARATOR_COLOR))
                .append(Component.text(formatNumber(max), LABEL_COLOR))
                .append(Component.text(" E", LABEL_COLOR))
                .build();

        Component throughput = throughputLine(type, tier);
        Component networkSummary = networkSummary(network);

        return Component.text()
                .append(header)
                .append(separator())
                .append(bar)
                .append(Component.text("  "))
                .append(pctText)
                .append(separator())
                .append(buffer)
                .append(throughput)
                .append(networkSummary)
                .build();
    }

    private static @NotNull Component buildBossBarTitle(
            @NotNull EnergyNodeType type, @NotNull EnergyTier tier,
            long current, long max, EnergyNetwork network) {
        TextColor accent = accentFor(type);
        Component header = Component.text()
                .append(Component.text(iconFor(type) + " ", accent))
                .append(Component.text(longType(type), accent).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" \u00b7 ", SEPARATOR_COLOR))
                .append(Component.text(tier.name(), tierColorFor(tier)))
                .build();
        Component buffer = Component.text()
                .append(Component.text("  "))
                .append(Component.text(formatNumber(current), VALUE_COLOR))
                .append(Component.text(" / ", SEPARATOR_COLOR))
                .append(Component.text(formatNumber(max), LABEL_COLOR))
                .append(Component.text(" E", LABEL_COLOR))
                .build();
        Component throughput = throughputLine(type, tier);
        Component networkSummary = networkSummary(network);
        return header.append(buffer).append(throughput).append(networkSummary);
    }

    private static @NotNull Component separator() {
        return Component.text(SEPARATOR, SEPARATOR_COLOR);
    }

    private static @NotNull Component throughputLine(@NotNull EnergyNodeType type, @NotNull EnergyTier tier) {
        return switch (type) {
            case GENERATOR -> Component.text()
                    .append(separator())
                    .append(Component.text("\u25b2 +", NamedTextColor.GREEN)
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(EnergySpecs.generationPerTick(tier) + " E/t",
                            NamedTextColor.GREEN))
                    .build();
            case CONSUMER -> Component.text()
                    .append(separator())
                    .append(Component.text("\u25bc -", NamedTextColor.RED)
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(EnergySpecs.consumptionPerTick(tier) + " E/t",
                            NamedTextColor.RED))
                    .build();
            case CAPACITOR, CABLE -> Component.empty();
        };
    }

    private static @NotNull Component networkSummary(EnergyNetwork network) {
        if (network == null) {
            return Component.text()
                    .append(separator())
                    .append(Component.text("isolated", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, true))
                    .build();
        }
        long stored = network.totalStored();
        long capacity = network.totalCapacity();
        long pct = capacity <= 0 ? 0 : Math.round(((double) stored / capacity) * 100);
        return Component.text()
                .append(separator())
                .append(Component.text("\u2756 ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("Net ", NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(network.size() + "n", VALUE_COLOR))
                .append(Component.text("  "))
                .append(Component.text(pct + "%", percentColor(pct)))
                .build();
    }

    private static @NotNull Component renderBar(long current, long max, @NotNull TextColor fill) {
        int filled = max <= 0 ? 0 : (int) Math.round(((double) current / max) * BAR_SEGMENTS);
        filled = Math.max(0, Math.min(BAR_SEGMENTS, filled));
        StringBuilder full = new StringBuilder(filled);
        for (int i = 0; i < filled; i++) full.append('\u2588');
        StringBuilder empty = new StringBuilder(BAR_SEGMENTS - filled);
        for (int i = 0; i < BAR_SEGMENTS - filled; i++) empty.append('\u2592'); // light shade
        return Component.text()
                .append(Component.text("\u2503", SEPARATOR_COLOR))
                .append(Component.text(full.toString(), fill))
                .append(Component.text(empty.toString(), NamedTextColor.DARK_GRAY))
                .append(Component.text("\u2503", SEPARATOR_COLOR))
                .build();
    }

    private static @NotNull TextColor accentFor(@NotNull EnergyNodeType type) {
        return switch (type) {
            case GENERATOR -> NamedTextColor.GOLD;
            case CONSUMER  -> NamedTextColor.RED;
            case CAPACITOR -> NamedTextColor.AQUA;
            case CABLE     -> NamedTextColor.WHITE;
        };
    }

    private static @NotNull TextColor tierColorFor(@NotNull EnergyTier tier) {
        return switch (tier.name().toUpperCase(Locale.ROOT)) {
            case "LOW"    -> NamedTextColor.GREEN;
            case "MEDIUM" -> NamedTextColor.YELLOW;
            case "HIGH"   -> NamedTextColor.GOLD;
            case "ULTRA", "EXTREME" -> NamedTextColor.LIGHT_PURPLE;
            default       -> NamedTextColor.GRAY;
        };
    }

    private static @NotNull TextColor percentColor(long pct) {
        if (pct >= 75) return NamedTextColor.GREEN;
        if (pct >= 25) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    private static @NotNull BossBar.Color bossColorFor(@NotNull EnergyNodeType type) {
        return switch (type) {
            case GENERATOR -> BossBar.Color.YELLOW;
            case CONSUMER  -> BossBar.Color.RED;
            case CAPACITOR -> BossBar.Color.BLUE;
            case CABLE     -> BossBar.Color.WHITE;
        };
    }

    private static @NotNull String iconFor(@NotNull EnergyNodeType type) {
        return switch (type) {
            case GENERATOR -> "\u26a1"; // ⚡
            case CONSUMER  -> "\u2699"; // ⚙
            case CAPACITOR -> "\u25c6"; // ◆
            case CABLE     -> "\u2500"; // ─
        };
    }

    private static @NotNull String longType(@NotNull EnergyNodeType type) {
        return switch (type) {
            case GENERATOR -> "GENERATOR";
            case CONSUMER  -> "CONSUMER";
            case CAPACITOR -> "CAPACITOR";
            case CABLE     -> "CABLE";
        };
    }

    private static @NotNull String formatNumber(long n) {
        // Insert a thin space every three digits for readability (e.g. 1 234 567).
        if (n < 1000) return Long.toString(n);
        String raw = Long.toString(Math.abs(n));
        StringBuilder out = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            int fromEnd = len - i;
            if (i > 0 && fromEnd % 3 == 0) out.append(' ');
            out.append(raw.charAt(i));
        }
        return n < 0 ? "-" + out : out.toString();
    }

    private static boolean isWrench(@NotNull ItemStack stack) {
        if (ITEM_ID_PDC == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        String id = stack.getItemMeta().getPersistentDataContainer()
                .get(ITEM_ID_PDC, PersistentDataType.STRING);
        return WRENCH_ID.equals(id);
    }
}
