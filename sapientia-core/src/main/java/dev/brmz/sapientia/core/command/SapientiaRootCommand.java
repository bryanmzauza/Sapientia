package dev.brmz.sapientia.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import dev.brmz.sapientia.core.SapientiaPlugin;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Root {@code /sapientia} command. Keeps dispatch intentionally minimal until the
 * Cloud framework bindings land in a later task.
 */
public final class SapientiaRootCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("help", "reload", "give");

    private final SapientiaPlugin plugin;
    private final ItemRegistry registry;

    public SapientiaRootCommand(@NotNull SapientiaPlugin plugin, @NotNull ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        Messages msg = plugin.messages();
        if (args.length == 0) {
            showHelp(sender, msg);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> showHelp(sender, msg);
            case "reload" -> handleReload(sender, msg);
            case "give" -> handleGive(sender, msg, args);
            default -> sender.sendMessage(msg.component("command.unknown"));
        }
        return true;
    }

    private void showHelp(CommandSender sender, Messages msg) {
        sender.sendMessage(msg.component("command.help.header"));
        sendHelpLine(sender, msg, "/sapientia help", "command.help.desc.help");
        sendHelpLine(sender, msg, "/sapientia reload", "command.help.desc.reload");
        sendHelpLine(sender, msg, "/sapientia give <player> <id> [amount]", "command.help.desc.give");
    }

    private void sendHelpLine(CommandSender sender, Messages msg, String usage, String descKey) {
        String description = msg.hasKey(descKey) ? msg.plain(descKey) : "";
        sender.sendMessage(msg.component("command.help.entry",
                Placeholder.parsed("usage", usage),
                Placeholder.parsed("description", description)));
    }

    private void handleReload(CommandSender sender, Messages msg) {
        if (!sender.hasPermission("sapientia.command.reload")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        long started = System.nanoTime();
        try {
            plugin.reloadConfig();
            String locale = plugin.getConfig().getString("locale", "en");
            plugin.messages().setActiveLocale(locale);
            long ms = (System.nanoTime() - started) / 1_000_000;
            sender.sendMessage(msg.component("command.reload.success",
                    Placeholder.parsed("ms", Long.toString(ms))));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Reload failed: " + e);
            sender.sendMessage(msg.component("command.reload.failure",
                    Placeholder.parsed("error", String.valueOf(e.getMessage()))));
        }
    }

    private void handleGive(CommandSender sender, Messages msg, String[] args) {
        if (!sender.hasPermission("sapientia.command.give")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(msg.component("command.give.usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(msg.component("command.give.player-not-found",
                    Placeholder.parsed("name", args[1])));
            return;
        }
        String id = args[2];
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(msg.component("command.give.invalid-amount",
                        Placeholder.parsed("value", args[3])));
                return;
            }
        }
        ItemStack stack = registry.createStack(id, amount);
        if (stack == null) {
            sender.sendMessage(msg.component("command.give.unknown-item",
                    Placeholder.parsed("id", id)));
            return;
        }
        target.getInventory().addItem(stack);
        sender.sendMessage(msg.component("command.give.success",
                Placeholder.parsed("player", target.getName()),
                Placeholder.parsed("id", id),
                Placeholder.parsed("amount", Integer.toString(amount))));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(registry.all().keySet()), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }
}
