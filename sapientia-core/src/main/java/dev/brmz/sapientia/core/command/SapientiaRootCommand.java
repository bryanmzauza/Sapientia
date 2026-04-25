package dev.brmz.sapientia.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.logic.LogicService;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.core.SapientiaPlugin;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.pack.ResourcePackBuilder;
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

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "reload", "give", "pack", "logistics", "fluids", "logic");

    private final SapientiaPlugin plugin;
    private final ItemRegistry registry;
    private final ContentOverrides overrides;
    private final ResourcePackBuilder packBuilder;
    private final LogicService logicService;

    public SapientiaRootCommand(@NotNull SapientiaPlugin plugin,
                                @NotNull ItemRegistry registry,
                                @NotNull ContentOverrides overrides,
                                @NotNull ResourcePackBuilder packBuilder,
                                @NotNull LogicService logicService) {
        this.plugin = plugin;
        this.registry = registry;
        this.overrides = overrides;
        this.packBuilder = packBuilder;
        this.logicService = logicService;
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
            case "reload" -> handleReload(sender, msg, args);
            case "give" -> handleGive(sender, msg, args);
            case "pack" -> handlePack(sender, msg, args);
            case "logistics" -> handleLogistics(sender, msg, args);
            case "fluids" -> handleFluids(sender, msg, args);
            case "logic" -> handleLogic(sender, msg, args);
            default -> sender.sendMessage(msg.component("command.unknown"));
        }
        return true;
    }

    private void showHelp(CommandSender sender, Messages msg) {
        sender.sendMessage(msg.component("command.help.header"));
        sendHelpLine(sender, msg, "/sapientia help", "command.help.desc.help");
        sendHelpLine(sender, msg, "/sapientia reload", "command.help.desc.reload");
        sendHelpLine(sender, msg, "/sapientia reload content", "command.help.desc.reload-content");
        sendHelpLine(sender, msg, "/sapientia give <player> <id> [amount]", "command.help.desc.give");
        sendHelpLine(sender, msg, "/sapientia pack build java", "command.help.desc.pack-build");
        sendHelpLine(sender, msg, "/sapientia pack build bedrock", "command.help.desc.pack-build-bedrock");
        sendHelpLine(sender, msg, "/sapientia pack build all", "command.help.desc.pack-build-all");
        sendHelpLine(sender, msg, "/sapientia logistics", "command.help.desc.logistics");
        sendHelpLine(sender, msg, "/sapientia logic", "command.help.desc.logic");
    }

    private void sendHelpLine(CommandSender sender, Messages msg, String usage, String descKey) {
        String description = msg.hasKey(descKey) ? msg.plain(descKey) : "";
        sender.sendMessage(msg.component("command.help.entry",
                Placeholder.parsed("usage", usage),
                Placeholder.parsed("description", description)));
    }

    private void handleReload(CommandSender sender, Messages msg, String[] args) {
        if (!sender.hasPermission("sapientia.command.reload")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("content")) {
            long startedContent = System.nanoTime();
            ContentOverrides.ReloadReport report = overrides.reload();
            long ms = (System.nanoTime() - startedContent) / 1_000_000;
            sender.sendMessage(msg.component("command.reload.content.success",
                    Placeholder.parsed("items", Integer.toString(report.items())),
                    Placeholder.parsed("blocks", Integer.toString(report.blocks())),
                    Placeholder.parsed("recipes", Integer.toString(report.recipes())),
                    Placeholder.parsed("ms", Long.toString(ms))));
            if (!report.issues().isEmpty()) {
                sender.sendMessage(msg.component("command.reload.content.issues",
                        Placeholder.parsed("count", Integer.toString(report.issues().size()))));
            }
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

    private void handlePack(CommandSender sender, Messages msg, String[] args) {
        if (!sender.hasPermission("sapientia.command.pack")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("build")) {
            sender.sendMessage(msg.component("command.pack.usage"));
            return;
        }
        String target = args[2].toLowerCase(java.util.Locale.ROOT);
        try {
            switch (target) {
                case "java" -> {
                    java.nio.file.Path output = packBuilder.buildJavaPack();
                    sender.sendMessage(msg.component("command.pack.success",
                            Placeholder.parsed("path", output.toString())));
                }
                case "bedrock" -> {
                    java.nio.file.Path output = packBuilder.buildBedrockPack();
                    sender.sendMessage(msg.component("command.pack.bedrock.success",
                            Placeholder.parsed("path", output.toString())));
                }
                case "all" -> {
                    java.nio.file.Path j = packBuilder.buildJavaPack();
                    sender.sendMessage(msg.component("command.pack.success",
                            Placeholder.parsed("path", j.toString())));
                    java.nio.file.Path b = packBuilder.buildBedrockPack();
                    sender.sendMessage(msg.component("command.pack.bedrock.success",
                            Placeholder.parsed("path", b.toString())));
                }
                default -> sender.sendMessage(msg.component("command.pack.usage"));
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Pack build failed: " + e);
            sender.sendMessage(msg.component("command.pack.failure",
                    Placeholder.parsed("error", String.valueOf(e.getMessage()))));
        }
    }

    private void handleLogistics(CommandSender sender, Messages msg, String[] args) {
        if (!sender.hasPermission("sapientia.command.logistics")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.component("command.players-only"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg.component("command.logistics.usage"));
            return;
        }
        org.bukkit.block.Block target = player.getTargetBlockExact(8);
        var logistics = dev.brmz.sapientia.api.Sapientia.get().logistics();
        var nodeOpt = target == null ? java.util.Optional.<dev.brmz.sapientia.api.logistics.ItemNode>empty()
                : logistics.nodeAt(target);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (nodeOpt.isEmpty()) {
                    sender.sendMessage(msg.component("command.logistics.info.none"));
                    return;
                }
                var node = nodeOpt.get();
                org.bukkit.block.Block b = node.block();
                int x = b == null ? 0 : b.getX();
                int y = b == null ? 0 : b.getY();
                int z = b == null ? 0 : b.getZ();
                sender.sendMessage(msg.component("command.logistics.info.header",
                        Placeholder.parsed("x", Integer.toString(x)),
                        Placeholder.parsed("y", Integer.toString(y)),
                        Placeholder.parsed("z", Integer.toString(z)),
                        Placeholder.parsed("node", node.nodeId().toString())));
                logistics.networkOf(node).ifPresent(net ->
                        sender.sendMessage(msg.component("command.logistics.info.network",
                                Placeholder.parsed("network", net.networkId().toString()),
                                Placeholder.parsed("count", Integer.toString(net.size())),
                                Placeholder.parsed("policy", net.routingPolicy().name()))));
                var rules = logistics.getFilterRules(node.nodeId());
                if (rules.isEmpty()) {
                    sender.sendMessage(msg.component("command.logistics.info.empty"));
                } else {
                    for (int i = 0; i < rules.size(); i++) {
                        var rule = rules.get(i);
                        sender.sendMessage(msg.component("command.logistics.info.rule",
                                Placeholder.parsed("index", Integer.toString(i)),
                                Placeholder.parsed("mode", rule.mode().name()),
                                Placeholder.parsed("pattern", rule.pattern())));
                    }
                }
            }
            case "policy" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logistics.policy.usage"));
                    return;
                }
                if (nodeOpt.isEmpty()) {
                    sender.sendMessage(msg.component("command.logistics.policy.not-found"));
                    return;
                }
                dev.brmz.sapientia.api.logistics.ItemRoutingPolicy policy;
                try {
                    policy = dev.brmz.sapientia.api.logistics.ItemRoutingPolicy
                            .valueOf(args[2].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(msg.component("command.logistics.policy.usage"));
                    return;
                }
                logistics.networkOf(nodeOpt.get()).ifPresent(net ->
                        logistics.setRoutingPolicy(net.networkId(), policy));
                sender.sendMessage(msg.component("command.logistics.policy.success",
                        Placeholder.parsed("policy", policy.name())));
            }
            case "filter" -> handleLogisticsFilter(sender, msg, args, nodeOpt);
            default -> sender.sendMessage(msg.component("command.logistics.usage"));
        }
    }

    private void handleFluids(CommandSender sender, Messages msg, String[] args) {
        if (!sender.hasPermission("sapientia.command.fluids")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.component("command.players-only"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg.component("command.fluids.usage"));
            return;
        }
        org.bukkit.block.Block target = player.getTargetBlockExact(8);
        var fluids = dev.brmz.sapientia.api.Sapientia.get().fluids();
        var nodeOpt = target == null ? java.util.Optional.<dev.brmz.sapientia.api.fluids.FluidNode>empty()
                : fluids.nodeAt(target);
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (nodeOpt.isEmpty()) {
                    sender.sendMessage(msg.component("command.fluids.info.none"));
                    return;
                }
                var node = nodeOpt.get();
                org.bukkit.block.Block b = node.block();
                int x = b == null ? 0 : b.getX();
                int y = b == null ? 0 : b.getY();
                int z = b == null ? 0 : b.getZ();
                sender.sendMessage(msg.component("command.fluids.info.header",
                        Placeholder.parsed("x", Integer.toString(x)),
                        Placeholder.parsed("y", Integer.toString(y)),
                        Placeholder.parsed("z", Integer.toString(z)),
                        Placeholder.parsed("node", node.nodeId().toString()),
                        Placeholder.parsed("type", node.type().name())));
                fluids.networkOf(node).ifPresent(net ->
                        sender.sendMessage(msg.component("command.fluids.info.network",
                                Placeholder.parsed("network", net.networkId().toString()),
                                Placeholder.parsed("count", Integer.toString(net.size())))));
                var contents = node.contents();
                if (contents == null || contents.isEmpty()) {
                    sender.sendMessage(msg.component("command.fluids.info.empty"));
                } else {
                    sender.sendMessage(msg.component("command.fluids.info.tank",
                            Placeholder.parsed("fluid", contents.type().id().toString()),
                            Placeholder.parsed("amount", Long.toString(contents.amountMb())),
                            Placeholder.parsed("capacity", Long.toString(node.capacityMb()))));
                }
            }
            default -> sender.sendMessage(msg.component("command.fluids.usage"));
        }
    }

    private void handleLogisticsFilter(CommandSender sender, Messages msg, String[] args,
                                       java.util.Optional<dev.brmz.sapientia.api.logistics.ItemNode> nodeOpt) {
        var logistics = dev.brmz.sapientia.api.Sapientia.get().logistics();
        if (args.length < 3) {
            sender.sendMessage(msg.component("command.logistics.filter.usage"));
            return;
        }
        if (nodeOpt.isEmpty() || nodeOpt.get().type()
                != dev.brmz.sapientia.api.logistics.ItemNodeType.FILTER) {
            sender.sendMessage(msg.component("command.logistics.filter.not-filter"));
            return;
        }
        var node = nodeOpt.get();
        var rules = new ArrayList<>(logistics.getFilterRules(node.nodeId()));
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (rules.isEmpty()) {
                    sender.sendMessage(msg.component("command.logistics.info.empty"));
                } else {
                    for (int i = 0; i < rules.size(); i++) {
                        var rule = rules.get(i);
                        sender.sendMessage(msg.component("command.logistics.info.rule",
                                Placeholder.parsed("index", Integer.toString(i)),
                                Placeholder.parsed("mode", rule.mode().name()),
                                Placeholder.parsed("pattern", rule.pattern())));
                    }
                }
            }
            case "clear" -> {
                logistics.setFilterRules(node.nodeId(), List.of());
                sender.sendMessage(msg.component("command.logistics.filter.cleared"));
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(msg.component("command.logistics.filter.usage"));
                    return;
                }
                int index;
                try {
                    index = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(msg.component("command.logistics.filter.bad-index",
                            Placeholder.parsed("value", args[3])));
                    return;
                }
                if (index < 0 || index >= rules.size()) {
                    sender.sendMessage(msg.component("command.logistics.filter.bad-index",
                            Placeholder.parsed("value", args[3])));
                    return;
                }
                rules.remove(index);
                // Reindex.
                List<dev.brmz.sapientia.api.logistics.ItemFilterRule> renum = new ArrayList<>(rules.size());
                for (int i = 0; i < rules.size(); i++) {
                    renum.add(new dev.brmz.sapientia.api.logistics.ItemFilterRule(
                            i, rules.get(i).mode(), rules.get(i).pattern()));
                }
                logistics.setFilterRules(node.nodeId(), renum);
                sender.sendMessage(msg.component("command.logistics.filter.removed",
                        Placeholder.parsed("index", Integer.toString(index))));
            }
            case "add" -> {
                if (args.length < 5) {
                    sender.sendMessage(msg.component("command.logistics.filter.usage"));
                    return;
                }
                dev.brmz.sapientia.api.logistics.ItemFilterMode mode;
                try {
                    mode = dev.brmz.sapientia.api.logistics.ItemFilterMode
                            .valueOf(args[3].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(msg.component("command.logistics.filter.bad-mode"));
                    return;
                }
                String pattern = args[4];
                rules.add(new dev.brmz.sapientia.api.logistics.ItemFilterRule(
                        rules.size(), mode, pattern));
                logistics.setFilterRules(node.nodeId(), rules);
                sender.sendMessage(msg.component("command.logistics.filter.added",
                        Placeholder.parsed("mode", mode.name()),
                        Placeholder.parsed("pattern", pattern)));
            }
            default -> sender.sendMessage(msg.component("command.logistics.filter.usage"));
        }
    }

    private void handleLogic(CommandSender sender, Messages msg, String[] args) {
        if (!sender.hasPermission("sapientia.command.logic")) {
            sender.sendMessage(msg.component("command.no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg.component("command.logic.usage"));
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                List<String> programs = logicService.list();
                sender.sendMessage(msg.component("command.logic.list.header",
                        Placeholder.parsed("count", Integer.toString(programs.size()))));
                for (String name : programs) {
                    sender.sendMessage(msg.component("command.logic.list.entry",
                            Placeholder.parsed("name", name),
                            Placeholder.parsed("enabled",
                                    Boolean.toString(logicService.isEnabled(name)))));
                }
            }
            case "info" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logic.usage"));
                    return;
                }
                String name = args[2];
                logicService.get(name).ifPresentOrElse(program -> {
                    sender.sendMessage(msg.component("command.logic.info.header",
                            Placeholder.parsed("name", name),
                            Placeholder.parsed("enabled",
                                    Boolean.toString(logicService.isEnabled(name)))));
                    sender.sendMessage(msg.component("command.logic.info.size",
                            Placeholder.parsed("nodes", Integer.toString(program.nodes().size())),
                            Placeholder.parsed("edges", Integer.toString(program.edges().size()))));
                }, () -> sender.sendMessage(msg.component("command.logic.unknown",
                        Placeholder.parsed("name", name))));
            }
            case "load" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logic.usage"));
                    return;
                }
                java.nio.file.Path source = plugin.getDataFolder().toPath()
                        .resolve("logic").resolve(args[2]);
                try {
                    String yaml = java.nio.file.Files.readString(source);
                    LogicProgram program = logicService.importYaml(yaml);
                    logicService.register(program);
                    sender.sendMessage(msg.component("command.logic.load.success",
                            Placeholder.parsed("name", program.name()),
                            Placeholder.parsed("file", source.getFileName().toString())));
                } catch (java.io.IOException ex) {
                    sender.sendMessage(msg.component("command.logic.load.io",
                            Placeholder.parsed("error", String.valueOf(ex.getMessage()))));
                } catch (RuntimeException ex) {
                    sender.sendMessage(msg.component("command.logic.load.invalid",
                            Placeholder.parsed("error", String.valueOf(ex.getMessage()))));
                }
            }
            case "unload" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logic.usage"));
                    return;
                }
                logicService.unregister(args[2]);
                sender.sendMessage(msg.component("command.logic.unload.success",
                        Placeholder.parsed("name", args[2])));
            }
            case "enable", "disable" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logic.usage"));
                    return;
                }
                String name = args[2];
                if (logicService.get(name).isEmpty()) {
                    sender.sendMessage(msg.component("command.logic.unknown",
                            Placeholder.parsed("name", name)));
                    return;
                }
                boolean enable = sub.equals("enable");
                logicService.setEnabled(name, enable);
                sender.sendMessage(msg.component(enable
                                ? "command.logic.enable.success"
                                : "command.logic.disable.success",
                        Placeholder.parsed("name", name)));
            }
            case "export" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logic.usage"));
                    return;
                }
                String name = args[2];
                if (logicService.get(name).isEmpty()) {
                    sender.sendMessage(msg.component("command.logic.unknown",
                            Placeholder.parsed("name", name)));
                    return;
                }
                java.nio.file.Path target = plugin.getDataFolder().toPath()
                        .resolve("logic").resolve(name + ".yml");
                try {
                    java.nio.file.Files.createDirectories(target.getParent());
                    java.nio.file.Files.writeString(target, logicService.exportYaml(name));
                    sender.sendMessage(msg.component("command.logic.export.success",
                            Placeholder.parsed("name", name),
                            Placeholder.parsed("path", target.toString())));
                } catch (java.io.IOException ex) {
                    sender.sendMessage(msg.component("command.logic.export.io",
                            Placeholder.parsed("error", String.valueOf(ex.getMessage()))));
                }
            }
            case "tick" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.component("command.logic.usage"));
                    return;
                }
                String name = args[2];
                if (logicService.get(name).isEmpty()) {
                    sender.sendMessage(msg.component("command.logic.unknown",
                            Placeholder.parsed("name", name)));
                    return;
                }
                try {
                    logicService.runOnce(name);
                    sender.sendMessage(msg.component("command.logic.tick.success",
                            Placeholder.parsed("name", name)));
                } catch (RuntimeException ex) {
                    sender.sendMessage(msg.component("command.logic.tick.failure",
                            Placeholder.parsed("error", String.valueOf(ex.getMessage()))));
                }
            }
            default -> sender.sendMessage(msg.component("command.logic.usage"));
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
        String rawId = args[2];
        // Accept bare ids (e.g. "wrench") as shorthand for "sapientia:wrench".
        // Explicit "vendor:id" is still respected; only the default namespace
        // is inferred when the argument has no colon.
        String id = rawId.indexOf(':') < 0 ? "sapientia:" + rawId : rawId;
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
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            return filter(List.of("content"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pack")) {
            return filter(List.of("build"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("build")) {
            return filter(List.of("java", "bedrock", "all"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("logistics")) {
            return filter(List.of("info", "policy", "filter"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("fluids")) {
            return filter(List.of("info"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("logic")) {
            return filter(List.of("list", "info", "load", "unload", "enable", "disable", "export", "tick"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("logic")) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (List.of("info", "unload", "enable", "disable", "export", "tick").contains(sub)) {
                return filter(logicService.list(), args[2]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("logistics")) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("policy")) {
                return filter(List.of("round_robin", "priority", "first_match"), args[2]);
            }
            if (sub.equals("filter")) {
                return filter(List.of("add", "remove", "clear", "list"), args[2]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("logistics")
                && args[1].equalsIgnoreCase("filter") && args[2].equalsIgnoreCase("add")) {
            return filter(List.of("whitelist", "blacklist"), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Show only the bare names (e.g. "wrench" instead of "sapientia:wrench").
            // The give handler auto-prefixes "sapientia:" when no colon is present,
            // so this stays unambiguous for the bundled catalog while still letting
            // power users type a fully-qualified vendor id manually.
            List<String> bareNames = new ArrayList<>();
            for (String fullId : registry.all().keySet()) {
                int colon = fullId.indexOf(':');
                bareNames.add(colon < 0 ? fullId : fullId.substring(colon + 1));
            }
            return filter(bareNames, args[2]);
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
