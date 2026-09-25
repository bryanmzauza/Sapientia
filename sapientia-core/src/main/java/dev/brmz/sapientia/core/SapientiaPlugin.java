package dev.brmz.sapientia.core;

import java.sql.SQLException;
import java.util.Optional;

import dev.brmz.sapientia.api.PlatformType;
import dev.brmz.sapientia.api.Sapientia;
import dev.brmz.sapientia.api.SapientiaAPI;
import dev.brmz.sapientia.api.Version;
import dev.brmz.sapientia.api.android.AndroidService;
import dev.brmz.sapientia.api.block.SapientiaBlock;
import dev.brmz.sapientia.api.crafting.RecipeRegistry;
import dev.brmz.sapientia.api.energy.EnergyNode;
import dev.brmz.sapientia.api.energy.EnergyService;
import dev.brmz.sapientia.api.guide.GuideEntry;
import dev.brmz.sapientia.api.guide.GuideService;
import dev.brmz.sapientia.api.guide.UnlockService;
import dev.brmz.sapientia.api.fluids.FluidService;
import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.api.logic.LogicService;
import dev.brmz.sapientia.api.logistics.ItemService;
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
import dev.brmz.sapientia.core.fluids.BuiltinFluidTypes;
import dev.brmz.sapientia.core.fluids.FluidNetworkGraph;
import dev.brmz.sapientia.core.fluids.FluidNodeStore;
import dev.brmz.sapientia.core.fluids.FluidServiceImpl;
import dev.brmz.sapientia.core.fluids.FluidSolver;
import dev.brmz.sapientia.core.logic.LogicProgramStore;
import dev.brmz.sapientia.core.logic.LogicServiceImpl;
import dev.brmz.sapientia.core.logistics.ItemNetworkGraph;
import dev.brmz.sapientia.core.logistics.ItemNodeStore;
import dev.brmz.sapientia.core.logistics.ItemServiceImpl;
import dev.brmz.sapientia.core.logistics.ItemSolver;
import dev.brmz.sapientia.core.guide.GuideServiceImpl;
import dev.brmz.sapientia.core.guide.UnlockServiceImpl;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.overrides.ContentOverrideService;
import dev.brmz.sapientia.core.pack.BundledPack;
import dev.brmz.sapientia.core.pack.ResourcePackBuilder;
import dev.brmz.sapientia.core.persistence.DatabaseManager;
import dev.brmz.sapientia.core.persistence.DatabaseWorker;
import dev.brmz.sapientia.core.persistence.WriteBehindQueue;
import dev.brmz.sapientia.core.platform.PlatformService;
import dev.brmz.sapientia.core.scheduler.SapientiaScheduler;
import dev.brmz.sapientia.core.engine.ActivityTracker;
import dev.brmz.sapientia.core.engine.ChunkHydrator;
import dev.brmz.sapientia.core.engine.ChunkLimitListener;
import dev.brmz.sapientia.core.engine.EngineConfig;
import dev.brmz.sapientia.core.engine.SapientiaEngine;
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
 * {@link Sapientia}. See docs/internal/implementation-plan.md.
 */
public final class SapientiaPlugin extends JavaPlugin implements SapientiaAPI {

    /** Main-thread time per tick for applying chunk reads from the database thread. */
    private static final long CHUNK_APPLY_BUDGET_NANOS = 2_000_000L;

    private Messages messages;
    private DatabaseManager database;
    private PlatformService platformService;
    private ItemRegistry itemRegistry;
    private SapientiaBlockRegistry blockRegistry;
    private SapientiaScheduler scheduler;
    private UIService uiService;
    private SapientiaEngine engine;
    private org.bukkit.scheduler.BukkitTask engineTask;
    private CustomBlockStore blockStore;
    private WriteBehindQueue writeBehindQueue;
    private DatabaseWorker databaseWorker;
    private java.util.concurrent.ExecutorService energyExecutor;
    private ChunkHydrator chunkHydrator;
    private ChunkBlockIndex chunkBlockIndex;
    private EnergyServiceImpl energyService;
    private EnergySolver energySolver;
    private ItemServiceImpl logisticsService;
    private ItemSolver logisticsSolver;
    private FluidServiceImpl fluidsService;
    private FluidSolver fluidsSolver;
    private LogicServiceImpl logicService;
    private SapientiaRecipeRegistry recipeRegistry;
    private UnlockServiceImpl unlockService;
    private GuideServiceImpl guideService;
    private ContentOverrideService overrideService;
    private ResourcePackBuilder resourcePackBuilder;
    private MachineRunningRegistry machineRunning;
    private MachineUIDescriptor machineUI;
    private dev.brmz.sapientia.api.machine.MachineRecipeRegistry machineRecipes;
    private dev.brmz.sapientia.core.machine.MachineProcessor machineProcessor;
    private dev.brmz.sapientia.core.petroleum.ReservoirService reservoirService;
    private dev.brmz.sapientia.core.petroleum.PetroleumTicker petroleumTicker;
    private dev.brmz.sapientia.core.electronics.ElectronicsTicker electronicsTicker;
    private dev.brmz.sapientia.core.geo.GeoTicker geoTicker;
    private dev.brmz.sapientia.core.logistics.LogisticsTicker logisticsTicker;
    private dev.brmz.sapientia.core.logistics.LogisticsConfig logisticsConfig;
    private dev.brmz.sapientia.core.android.AndroidServiceImpl androidService;
    private dev.brmz.sapientia.core.android.AndroidTicker androidTicker;
    private dev.brmz.sapientia.core.android.AndroidConfig androidConfig;

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
        // One background thread owns every database write and chunk read (Foundation 1).
        this.databaseWorker = new DatabaseWorker(getLogger());
        this.writeBehindQueue = new WriteBehindQueue(getLogger(), database.dataSource());
        this.databaseWorker.register(writeBehindQueue);
        this.blockStore.attachWriteBehind(writeBehindQueue);
        this.chunkBlockIndex = new ChunkBlockIndex(getLogger(), blockStore, blockRegistry);
        // Single budgeted tick loop for machines and network solvers (Foundation 1).
        this.engine = new SapientiaEngine(getLogger(), EngineConfig.from(getConfig()));
        this.chunkBlockIndex.addObserver(engine);

        // Energy graph + solver (T-141, T-142 / 0.3.0).
        NetworkGraph energyGraph = new NetworkGraph();
        EnergyNodeStore energyStore = new EnergyNodeStore(getLogger(), database.dataSource());
        this.databaseWorker.register(energyStore.writes());
        this.energyService = new EnergyServiceImpl(energyGraph, energyStore);
        energyGraph.setActivityFilter(engine);
        engine.addActivityListener(energyGraph);
        this.energySolver = new EnergySolver(energyGraph);
        this.energyExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Sapientia-Energy");
            t.setDaemon(true);
            return t;
        });
        this.energySolver.runOn(energyExecutor, getLogger());

        // Item logistics graph + solver (T-300 / 1.1.0).
        ItemNetworkGraph logisticsGraph = new ItemNetworkGraph();
        ItemNodeStore itemStore = new ItemNodeStore(getLogger(), database.dataSource());
        this.databaseWorker.register(itemStore.writes());
        this.logisticsService = new ItemServiceImpl(logisticsGraph, itemStore);
        this.logisticsService.runDatabaseWorkOn(databaseWorker::execute);
        logisticsGraph.setActivityFilter(engine);
        engine.addActivityListener(logisticsGraph);
        this.logisticsSolver = new ItemSolver(
                logisticsGraph, itemRegistry, logisticsService::getFilterRules);

        // Fluid logistics graph + solver (T-301 / 1.2.0).
        FluidNetworkGraph fluidsGraph = new FluidNetworkGraph();
        FluidNodeStore fluidStore = new FluidNodeStore(getLogger(), database.dataSource());
        this.databaseWorker.register(fluidStore.writes());
        this.fluidsService = new FluidServiceImpl(getLogger(), fluidsGraph, fluidStore);
        fluidsGraph.setActivityFilter(engine);
        engine.addActivityListener(fluidsGraph);
        this.fluidsService.registerType(BuiltinFluidTypes.WATER);
        this.fluidsService.registerType(BuiltinFluidTypes.LAVA);
        this.fluidsService.registerType(BuiltinFluidTypes.MILK);
        this.fluidsService.registerType(BuiltinFluidTypes.CRUDE_OIL);
        this.fluidsService.registerType(BuiltinFluidTypes.DIESEL);
        this.fluidsService.registerType(BuiltinFluidTypes.GASOLINE);
        this.fluidsService.registerType(BuiltinFluidTypes.LUBRICANT);
        this.fluidsService.registerType(BuiltinFluidTypes.NUTRIENT_BROTH);
        // Gases (T-426 / 1.6.0).
        this.fluidsService.registerType(BuiltinFluidTypes.HYDROGEN);
        this.fluidsService.registerType(BuiltinFluidTypes.OXYGEN_GAS);
        this.fluidsService.registerType(BuiltinFluidTypes.NITROGEN);
        this.fluidsService.registerType(BuiltinFluidTypes.CHLORINE);
        this.fluidsService.registerType(BuiltinFluidTypes.ETHYLENE);
        this.fluidsService.registerType(BuiltinFluidTypes.COMPRESSED_AIR);
        // Atmospheric gases & cryogenics (T-435 / 1.7.0).
        this.fluidsService.registerType(BuiltinFluidTypes.ARGON);
        this.fluidsService.registerType(BuiltinFluidTypes.CARBON_DIOXIDE);
        this.fluidsService.registerType(BuiltinFluidTypes.LIQUID_OXYGEN);
        this.fluidsSolver = new FluidSolver(fluidsGraph, fluidsService);
        this.databaseWorker.start();

        // Machine recipe processor (T-404 / 1.4.1) + petroleum kinetic loop (T-412..T-415 / 1.5.1).
        this.machineRecipes = new dev.brmz.sapientia.api.machine.MachineRecipeRegistry();
        this.machineProcessor = new dev.brmz.sapientia.core.machine.MachineProcessor(
                energyService, machineRecipes);
        this.reservoirService = new dev.brmz.sapientia.core.petroleum.ReservoirService(
                getLogger(), database.dataSource());
        this.petroleumTicker = new dev.brmz.sapientia.core.petroleum.PetroleumTicker(
                this, energyService, fluidsService, reservoirService);

        // HV / electronics / gas kinetic loop (T-425 / T-426 / T-429 / 1.6.1).
        this.electronicsTicker = new dev.brmz.sapientia.core.electronics.ElectronicsTicker(
                this, energyService, fluidsService);

        // Geo & atmosphere kinetic loop (T-431..T-435 / 1.7.1).
        this.geoTicker = new dev.brmz.sapientia.core.geo.GeoTicker(
                this, energyService, fluidsService);

        // Advanced-logistics kinetic loop + maxflow opt-in (T-444 / T-450 / 1.8.1).
        this.logisticsConfig = dev.brmz.sapientia.core.logistics.LogisticsConfig.from(getConfig());
        this.logisticsTicker = new dev.brmz.sapientia.core.logistics.LogisticsTicker(
                getLogger(), this, logisticsGraph);

        // Programmable-logic DAG runtime (T-302 / 1.3.0).
        this.logicService = new LogicServiceImpl(
                getLogger(), new LogicProgramStore(getLogger(), database.dataSource()));
        this.logicService.hydrate();

        // Android service (T-451 / T-456 / 1.9.0). Hydrate is deferred until
        // after Sapientia.register() so onPlace lookups via Sapientia.get()
        // succeed for any chunk that was already loaded at enable time.
        this.androidConfig  = dev.brmz.sapientia.core.android.AndroidConfig.from(getConfig());
        this.androidService = new dev.brmz.sapientia.core.android.AndroidServiceImpl(
                getLogger(),
                new dev.brmz.sapientia.core.android.AndroidStore(getLogger(), database.dataSource()),
                this.logicService,
                this.androidConfig);
        this.androidTicker = new dev.brmz.sapientia.core.android.AndroidTicker(
                getLogger(), this.androidService);
        this.androidTicker.setActivityFilter(engine);

        // Crafting + guide + unlocks (T-130 / T-131 / T-150 / T-151 / 0.4.0).
        this.recipeRegistry = new SapientiaRecipeRegistry(itemRegistry);
        this.unlockService = new UnlockServiceImpl(getLogger(), database.dataSource());

        // Content overrides + resource pack pipeline (T-160..T-164 / 0.5.0).
        this.overrideService = new ContentOverrideService(
                getLogger(), getDataFolder().toPath().resolve("overrides"));
        this.overrideService.start();
        this.itemRegistry.setOverrides(overrideService);
        this.recipeRegistry.setOverrides(overrideService);
        BundledPack bundledPack = BundledPack.fromClassLoader(getClass().getClassLoader());
        if (getConfig().getBoolean("resource-pack.item-models", true)) {
            this.itemRegistry.setItemModels(bundledPack.itemModelIds());
        }
        this.resourcePackBuilder = new ResourcePackBuilder(
                getLogger(),
                getDataFolder().toPath(),
                bundledPack,
                getConfig().getInt("resource-pack.pack-format", ResourcePackBuilder.DEFAULT_PACK_FORMAT),
                version());
        this.resourcePackBuilder.setMessages(messages);
        this.resourcePackBuilder.setItemRegistry(itemRegistry);
        this.resourcePackBuilder.setGeyserFolder(() -> Optional
                .ofNullable(getServer().getPluginManager().getPlugin("Geyser-Spigot"))
                .map(geyser -> geyser.getDataFolder().toPath()));

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

        // Item filter UI (T-300 / 1.1.0). Always-on now that the logistics solver is wired.
        this.uiService.register(new dev.brmz.sapientia.core.ui.FilterDescriptor(messages));

        // Android program selector UI (T-453 / 1.9.1).
        this.uiService.register(new dev.brmz.sapientia.core.ui.AndroidProgramSelectorUI(messages));

        getServer().getPluginManager().registerEvents(uiService, this);
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
                    this, itemRegistry, overrideService, resourcePackBuilder, logicService);
            rootCommand.setExecutor(executor);
            rootCommand.setTabCompleter(executor);
        }

        Sapientia.register(this);

        // Now that the API is published, drain persisted androids and wire
        // the cap listener (T-451 / T-456 / 1.9.0).
        this.androidService.hydrate();
        getServer().getPluginManager().registerEvents(
                new dev.brmz.sapientia.core.android.AndroidCapsListener(this.androidService), this);

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

        // Machine behaviours must be known before chunks are indexed (Foundation 1).
        petroleumTicker.registerBehaviors(engine);
        electronicsTicker.registerBehaviors(engine);
        geoTicker.registerBehaviors(engine);
        logisticsTicker.registerBehaviors(engine);
        machineProcessor.registerBehaviors(engine, blockRegistry.all().values());

        ActivityTracker activityTracker = new ActivityTracker(engine.activity(), engine::currentTick);
        getServer().getPluginManager().registerEvents(activityTracker, this);
        activityTracker.seed(getServer().getOnlinePlayers());
        getServer().getPluginManager().registerEvents(
                new ChunkLimitListener(chunkBlockIndex, engine, messages), this);

        // Chunk state is read on the database thread and applied at the start of each tick.
        this.chunkHydrator = new ChunkHydrator(chunkBlockIndex, blockStore, energyService,
                logisticsService, fluidsService, databaseWorker);
        getServer().getPluginManager().registerEvents(chunkHydrator, this);
        // Chunks already loaded (reload case) are read here, on the main thread, once.
        for (org.bukkit.World world : getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                chunkHydrator.loadNow(world.getName(), chunk.getX(), chunk.getZ());
            }
        }

        // Periodic network work runs inside the engine's single tick loop, where
        // it is timed for /sapientia perf. Offsets keep the tasks on different ticks.
        engine.addSystemTask("chunks", 1, 0, () -> chunkHydrator.drain(CHUNK_APPLY_BUDGET_NANOS));
        engine.addSystemTask("energy", 10, 10, () -> {
            energySolver.tick();
            energyService.persistDirty();
        });
        engine.addSystemTask("logistics", 1, 5, logisticsSolver::tick);
        engine.addSystemTask("fluids", 5, 7, () -> {
            fluidsSolver.tick();
            fluidsService.persistDirty();
        });
        engine.addSystemTask("logic", 5, 9, logicService::tickAll);
        engine.addSystemTask("androids", 1, 21, androidTicker::tick);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onWorldUnload(org.bukkit.event.world.WorldUnloadEvent event) {
                engine.clearWorldCache();
            }
        }, this);
        this.engineTask = getServer().getScheduler().runTaskTimer(this, engine::tick, 1L, 1L);

        getLogger().info(messages.plain("plugin.enabled",
                Placeholder.parsed("version", getPluginMeta().getVersion())));
    }

    @Override
    public void onDisable() {
        Sapientia.unregister();
        if (engineTask != null) {
            engineTask.cancel();
        }
        if (uiService != null) {
            uiService.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (energyExecutor != null) {
            energyExecutor.shutdown();
            try {
                energyExecutor.awaitTermination(5L, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (energyService != null) {
            energyService.persistDirty();
        }
        if (fluidsService != null) {
            fluidsService.persistDirty();
        }
        if (databaseWorker != null) {
            databaseWorker.shutdown(); // writes everything still queued
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

    public @NotNull SapientiaEngine engine() {
        return engine;
    }

    public @NotNull CustomBlockStore blockStore() {
        return blockStore;
    }

    public @NotNull ChunkBlockIndex chunkBlockIndex() {
        return chunkBlockIndex;
    }

    public @NotNull dev.brmz.sapientia.core.machine.MachineProcessor machineProcessor() {
        return machineProcessor;
    }

    public @NotNull dev.brmz.sapientia.core.petroleum.ReservoirService reservoirService() {
        return reservoirService;
    }

    public @NotNull dev.brmz.sapientia.core.petroleum.PetroleumTicker petroleumTicker() {
        return petroleumTicker;
    }

    public @NotNull dev.brmz.sapientia.core.electronics.ElectronicsTicker electronicsTicker() {
        return electronicsTicker;
    }

    public @NotNull dev.brmz.sapientia.core.geo.GeoTicker geoTicker() {
        return geoTicker;
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

    /** Networks and network blocks loaded, for /sapientia perf: energy, item, fluid. */
    public int @NotNull [] networkCounts() {
        return new int[] {
                energyService.graph().networkCount(), energyService.graph().nodeCount(),
                logisticsService.graph().networkCount(), logisticsService.graph().nodeCount(),
                fluidsService.graph().networkCount(), fluidsService.graph().nodeCount()};
    }

    @Override
    public @NotNull EnergyService energy() {
        return energyService;
    }

    @Override
    public @NotNull ItemService logistics() {
        return logisticsService;
    }

    @Override
    public @NotNull FluidService fluids() {
        return fluidsService;
    }

    @Override
    public @NotNull LogicService logic() {
        return logicService;
    }

    @Override
    public @NotNull AndroidService androids() {
        return androidService;
    }

    @Override
    public @NotNull RecipeRegistry recipes() {
        return recipeRegistry;
    }

    @Override
    public @NotNull dev.brmz.sapientia.api.machine.MachineRecipeRegistry machineRecipes() {
        return machineRecipes;
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
