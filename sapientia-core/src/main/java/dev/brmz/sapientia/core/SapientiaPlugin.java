package dev.brmz.sapientia.core;

import java.sql.SQLException;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.Version;
import dev.brmz.sapientia.bedrock.BedrockFormsUIProvider;
import dev.brmz.sapientia.core.block.CustomBlockStore;
import dev.brmz.sapientia.core.command.SapientiaRootCommand;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.persistence.DatabaseManager;
import dev.brmz.sapientia.core.platform.PlatformService;
import dev.brmz.sapientia.core.scheduler.SapientiaScheduler;
import dev.brmz.sapientia.core.tick.TickBucketing;
import dev.brmz.sapientia.core.ui.JavaInventoryUIProvider;
import dev.brmz.sapientia.core.ui.UIService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Core plugin entry point. Wires up i18n, persistence, platform detection, item registry,
 * the scheduler adapter and the root command, then exposes the public API via
 * {@link Sapientia}. See docs/implementation-plan.md.
 */
public final class SapientiaPlugin extends JavaPlugin implements SapientiaAPI {

    private Messages messages;
    private DatabaseManager database;
    private PlatformService platformService;
    private ItemRegistry itemRegistry;
    private SapientiaScheduler scheduler;
    private UIService uiService;
    private TickBucketing tickBucketing;
    private CustomBlockStore blockStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(getLogger());
        this.messages.loadBundled(this);
        this.messages.setActiveLocale(getConfig().getString("locale", "en"));

        try {
            this.database = new DatabaseManager(getLogger(), getDataFolder().toPath().resolve("data"));
            this.database.start(getConfig().getInt("persistence.pool-size", 4));
        } catch (SQLException e) {
            getLogger().severe("Failed to start SQLite persistence: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.platformService = new PlatformService(getLogger(), database.dataSource());
        this.itemRegistry = new ItemRegistry(this, messages);
        this.scheduler = SapientiaScheduler.create(this);
        this.blockStore = new CustomBlockStore(getLogger(), database.dataSource());
        this.tickBucketing = new TickBucketing(getLogger(), scheduler);
        this.tickBucketing.start();

        this.uiService = new UIService(platformService);
        this.uiService.registerProvider(new JavaInventoryUIProvider(uiService));
        if (platformService.floodgateAvailable()) {
            this.uiService.registerProvider(new BedrockFormsUIProvider(getLogger()));
        }
        getServer().getPluginManager().registerEvents(uiService, this);
        getServer().getPluginManager().registerEvents(
                new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                        platformService.resolveAndEmit(event.getPlayer());
                    }
                }, this);

        PluginCommand rootCommand = getCommand("sapientia");
        if (rootCommand != null) {
            SapientiaRootCommand executor = new SapientiaRootCommand(this, itemRegistry);
            rootCommand.setExecutor(executor);
            rootCommand.setTabCompleter(executor);
        }

        Sapientia.register(this);

        getLogger().info(messages.plain("plugin.enabled",
                Placeholder.parsed("version", getPluginMeta().getVersion())));
    }

    @Override
    public void onDisable() {
        Sapientia.unregister();
        if (tickBucketing != null) {
            tickBucketing.stop();
        }
        if (uiService != null) {
            uiService.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (database != null) {
            database.close();
        }
        if (messages != null) {
            getLogger().info(messages.plain("plugin.disabled"));
        }
    }

    public @NotNull Messages messages() {
        return messages;
    }

    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    public @NotNull SapientiaScheduler scheduler() {
        return scheduler;
    }

    public @NotNull PlatformService platformService() {
        return platformService;
    }

    public @NotNull UIService uiService() {
        return uiService;
    }

    public @NotNull TickBucketing tickBucketing() {
        return tickBucketing;
    }

    public @NotNull CustomBlockStore blockStore() {
        return blockStore;
    }

    @Override
    public @NotNull Version version() {
        return Version.parse(getPluginMeta().getVersion());
    }

    @Override
    public @NotNull PlatformType platformOf(@NotNull Player player) {
        return platformService.resolve(player);
    }

    @Override
    public boolean isFloodgateAvailable() {
        return platformService != null && platformService.floodgateAvailable();
    }
}
