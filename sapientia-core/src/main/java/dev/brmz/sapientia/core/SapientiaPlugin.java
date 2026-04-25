package dev.brmz.sapientia.core;

import java.sql.SQLException;
import java.util.Optional;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.Version;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.crafting.RecipeRegistry;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyService;
import dev.brmz.sapientia.api.guide.GuideEntry;
import dev.brmz.sapientia.api.guide.GuideService;
import dev.brmz.sapientia.api.guide.UnlockService;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.overrides.ContentOverrides;
import dev.brmz.sapientia.bedrock.BedrockFormsUIProvider;
import dev.brmz.sapientia.core.block.BlockLifecycleListener;
import dev.brmz.sapientia.core.block.ChunkBlockIndex;
import dev.brmz.sapientia.core.block.CustomBlockStore;
import dev.brmz.sapientia.core.block.SapientiaBlockRegistry;
import dev.brmz.sapientia.core.command.SapientiaRootCommand;
import dev.brmz.sapientia.core.crafting.SapientiaRecipeRegistry;
import dev.brmz.sapientia.core.crafting.WorkbenchListener;
import dev.brmz.sapientia.core.energy.EnergyNodeStore;
import dev.brmz.sapientia.core.energy.EnergyServiceImpl;
import dev.brmz.sapientia.core.energy.EnergySolver;
import dev.brmz.sapientia.core.energy.NetworkGraph;
import dev.brmz.sapientia.core.guide.GuideServiceImpl;
import dev.brmz.sapientia.core.guide.UnlockServiceImpl;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.overrides.ContentOverrideService;
import dev.brmz.sapientia.core.pack.ResourcePackBuilder;
import dev.brmz.sapientia.core.persistence.DatabaseManager;
import dev.brmz.sapientia.core.persistence.WriteBehindQueue;
import dev.brmz.sapientia.core.platform.PlatformService;
import dev.brmz.sapientia.core.scheduler.SapientiaScheduler;
import dev.brmz.sapientia.core.tick.TickBucketing;
import dev.brmz.sapientia.core.ui.JavaInventoryUIProvider;
import dev.brmz.sapientia.core.ui.MachineJavaRenderer;
import dev.brmz.sapientia.core.ui.MachineRunningRegistry;
import dev.brmz.sapientia.core.ui.MachineUIDescriptor;
import dev.brmz.sapientia.core.ui.UIService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    private SapientiaBlockRegistry blockRegistry;
    private SapientiaScheduler scheduler;
    private UIService uiService;
    private TickBucketing tickBucketing;
    private CustomBlockStore blockStore;
    private WriteBehindQueue writeBehindQueue;
    private ChunkBlockIndex chunkBlockIndex;
    private EnergyServiceImpl energyService;
    private EnergySolver energySolver;
    private SapientiaRecipeRegistry recipeRegistry;
    private UnlockServiceImpl unlockService;
    private GuideServiceImpl guideService;
    private ContentOverrideService overrideService;
    private ResourcePackBuilder resourcePackBuilder;
    private MachineRunningRegistry machineRunning;
    private MachineUIDescriptor machineUI;

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
        this.blockRegistry = new SapientiaBlockRegistry();
        this.scheduler = SapientiaScheduler.create(this);
        this.blockStore = new CustomBlockStore(getLogger(), database.dataSource());
        this.writeBehindQueue = new WriteBehindQueue(getLogger(), database.dataSource());
        this.writeBehindQueue.start();
        this.blockStore.attachWriteBehind(writeBehindQueue);
        this.chunkBlockIndex = new ChunkBlockIndex(getLogger(), blockStore, blockRegistry);
        this.tickBucketing = new TickBucketing(getLogger(), scheduler);
        this.tickBucketing.start();

        // Energy graph + solver (T-141, T-142 / 0.3.0).
        NetworkGraph energyGraph = new NetworkGraph();
        this.energyService = new EnergyServiceImpl(
                energyGraph, new EnergyNodeStore(getLogger(), database.dataSource()));
        this.energySolver = new EnergySolver(energyGraph);

        // Crafting + guide + unlocks (T-130 / T-131 / T-150 / T-151 / 0.4.0).
        this.recipeRegistry = new SapientiaRecipeRegistry(itemRegistry);
        this.unlockService = new UnlockServiceImpl(getLogger(), database.dataSource());

        // Content overrides + resource pack pipeline (T-160..T-164 / 0.5.0).
        this.overrideService = new ContentOverrideService(
                getLogger(), getDataFolder().toPath().resolve("overrides"));
        this.overrideService.start();
        this.itemRegistry.setOverrides(overrideService);
        this.recipeRegistry.setOverrides(overrideService);
        this.resourcePackBuilder = new ResourcePackBuilder(
                getLogger(),
                getDataFolder().toPath().resolve("pack"),
                getConfig().getInt("resource-pack.pack-format", 32));
        this.resourcePackBuilder.setMessages(messages);
        this.resourcePackBuilder.setItemRegistry(itemRegistry);

        this.uiService = new UIService(platformService);
        this.uiService.registerProvider(new JavaInventoryUIProvider(uiService));
        if (platformService.floodgateAvailable()) {
            this.uiService.registerProvider(new BedrockFormsUIProvider(getLogger()));
        }

        // Machine UI (T-145 / T-202).
        this.machineRunning = new MachineRunningRegistry();
        this.machineUI = new MachineUIDescriptor(
                machineRunning,
                new MachineJavaRenderer(messages, machineRunning),
                platformService.floodgateAvailable()
                        ? new dev.brmz.sapientia.core.ui.MachineBedrockRenderer(messages, machineRunning)
                        : null);
        this.uiService.register(machineUI);

        // Experimental filter UI (T-203, opt-in via config flag).
        if (getConfig().getBoolean("experimental.filter", false)) {
            this.uiService.register(
                    new dev.brmz.sapientia.core.ui.FilterDescriptor(messages));
            getLogger().info("Experimental filter UI enabled (sapientia:filter).");
        }

        getServer().getPluginManager().registerEvents(uiService, this);
        getServer().getPluginManager().registerEvents(chunkBlockIndex, this);
        getServer().getPluginManager().registerEvents(
                new BlockLifecycleListener(itemRegistry, blockRegistry, chunkBlockIndex, blockStore),
                this);
        getServer().getPluginManager().registerEvents(
                new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                        platformService.resolveAndEmit(event.getPlayer());
                    }
                }, this);

        PluginCommand rootCommand = getCommand("sapientia");
        if (rootCommand != null) {
            SapientiaRootCommand executor = new SapientiaRootCommand(
                    this, itemRegistry, overrideService, resourcePackBuilder);
            rootCommand.setExecutor(executor);
            rootCommand.setTabCompleter(executor);
        }

        Sapientia.register(this);

        // Guide service depends on UIService + UnlockService + Messages being up.
        this.guideService = new GuideServiceImpl(this, uiService, unlockService, messages);

        // Workbench listener must exist before content registers recipes/blocks.
        WorkbenchListener workbench = new WorkbenchListener(this, recipeRegistry, unlockService);
        this.recipeRegistry.attachWorkbench(workbench);
        getServer().getPluginManager().registerEvents(workbench, this);

        // Register bundled demo content (ADR-012 / T-180 / T-130 / T-143).
        dev.brmz.sapientia.content.ContentBootstrap.registerAll(this, this);

        // Auto-populate guide entries from everything registered so far (T-150).
        for (SapientiaItem registered : itemRegistry.allSapientiaItems().values()) {
            guideService.register(new GuideEntry(
                    registered.id(),
                    registered.guideCategory(),
                    registered.displayNameKey(),
                    registered.baseMaterial(),
                    registered.discoveredByDefault()));
        }
        for (SapientiaBlock registered : blockRegistry.all().values()) {
            if (guideService.find(registered.id()).isPresent()) continue;
            guideService.register(new GuideEntry(
                    registered.id(),
                    registered.guideCategory(),
                    registered.displayNameKey(),
                    registered.baseMaterial(),
                    registered.discoveredByDefault()));
        }

        // Hydrate already-loaded chunks (reload / /plugman load case).
        for (org.bukkit.World world : getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                chunkBlockIndex.hydrate(world.getName(), chunk.getX(), chunk.getZ());
                energyService.hydrateChunk(world.getName(), chunk.getX(), chunk.getZ());
            }
        }

        // Energy chunk hooks.
        getServer().getPluginManager().registerEvents(
                new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
                        energyService.hydrateChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                    }
                    @org.bukkit.event.EventHandler
                    public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
                        energyService.unloadChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                    }
                }, this);

        // Energy tick — every 10 ticks (twice per second). Persists dirty nodes after each pass.
        getServer().getScheduler().runTaskTimer(this, () -> {
            energySolver.tick();
            energyService.persistDirty();
        }, 10L, 10L);

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
        if (writeBehindQueue != null) {
            writeBehindQueue.shutdown();
        }
        if (energyService != null) {
            energyService.persistDirty();
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

    public @NotNull SapientiaBlockRegistry blockRegistry() {
        return blockRegistry;
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

    public @NotNull ChunkBlockIndex chunkBlockIndex() {
        return chunkBlockIndex;
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

    // --- Content registry (ADR-012) --------------------------------------------------

    @Override
    public void registerItem(@NotNull SapientiaItem item) {
        itemRegistry.register(item);
    }

    @Override
    public void registerBlock(@NotNull SapientiaBlock block) {
        blockRegistry.register(block);
        // Auto-register a companion item form so `createStack(block.itemId())` works
        // for crafting results, `/sapientia give`, and block drops (BlockLifecycleListener).
        String itemId = block.itemId().toString();
        if (itemRegistry.get(itemId).isEmpty()) {
            itemRegistry.register(new ItemRegistry.ItemDefinition(
                    itemId,
                    block.baseMaterial(),
                    block.displayNameKey(),
                    java.util.List.of()));
        }
    }

    @Override
    public @NotNull Optional<SapientiaItem> findItem(@NotNull NamespacedKey id) {
        return itemRegistry.find(id);
    }

    @Override
    public @NotNull Optional<SapientiaBlock> findBlock(@NotNull NamespacedKey id) {
        return blockRegistry.find(id);
    }

    @Override
    public @NotNull Optional<ItemStack> createStack(@NotNull NamespacedKey id, int amount) {
        return Optional.ofNullable(itemRegistry.createStack(id.toString(), amount));
    }

    @Override
    public @NotNull EnergyService energy() {
        return energyService;
    }

    @Override
    public @NotNull RecipeRegistry recipes() {
        return recipeRegistry;
    }

    @Override
    public @NotNull GuideService guide() {
        return guideService;
    }

    @Override
    public @NotNull UnlockService unlocks() {
        return unlockService;
    }

    @Override
    public @NotNull ContentOverrides overrides() {
        return overrideService;
    }

    @Override
    public void openMachineUI(@NotNull Player player, @NotNull EnergyNode node) {
        uiService.open(player, machineUI, node);
    }

    @Override
    public void openUI(@NotNull Player player, @NotNull NamespacedKey key, @NotNull Object context) {
        uiService.open(player, key, context);
    }
}
