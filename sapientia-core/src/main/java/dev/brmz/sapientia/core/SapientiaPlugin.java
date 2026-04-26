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

        // Item logistics graph + solver (T-300 / 1.1.0).
        ItemNetworkGraph logisticsGraph = new ItemNetworkGraph();
        this.logisticsService = new ItemServiceImpl(
                logisticsGraph, new ItemNodeStore(getLogger(), database.dataSource()));
        this.logisticsSolver = new ItemSolver(
                logisticsGraph, itemRegistry, logisticsService::getFilterRules);

        // Fluid logistics graph + solver (T-301 / 1.2.0).
        FluidNetworkGraph fluidsGraph = new FluidNetworkGraph();
        this.fluidsService = new FluidServiceImpl(
                getLogger(), fluidsGraph, new FluidNodeStore(getLogger(), database.dataSource()));
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

        // Machine recipe processor (T-404 / 1.4.1) + petroleum kinetic loop (T-412..T-415 / 1.5.1).
        this.machineRecipes = new dev.brmz.sapientia.api.machine.MachineRecipeRegistry();
        this.machineProcessor = new dev.brmz.sapientia.core.machine.MachineProcessor(
                getLogger(), energyService, machineRecipes, chunkBlockIndex);
        this.reservoirService = new dev.brmz.sapientia.core.petroleum.ReservoirService(
                getLogger(), database.dataSource());
        this.petroleumTicker = new dev.brmz.sapientia.core.petroleum.PetroleumTicker(
                getLogger(), this, energyService, fluidsService, chunkBlockIndex, reservoirService);

        // HV / electronics / gas kinetic loop (T-425 / T-426 / T-429 / 1.6.1).
        this.electronicsTicker = new dev.brmz.sapientia.core.electronics.ElectronicsTicker(
                getLogger(), this, energyService, fluidsService, chunkBlockIndex);

        // Geo & atmosphere kinetic loop (T-431..T-435 / 1.7.1).
        this.geoTicker = new dev.brmz.sapientia.core.geo.GeoTicker(
                getLogger(), this, energyService, fluidsService, chunkBlockIndex);

        // Advanced-logistics kinetic loop + maxflow opt-in (T-444 / T-450 / 1.8.1).
        this.logisticsConfig = dev.brmz.sapientia.core.logistics.LogisticsConfig.from(getConfig());
        this.logisticsTicker = new dev.brmz.sapientia.core.logistics.LogisticsTicker(
                getLogger(), this, logisticsGraph, chunkBlockIndex);

        // Programmable-logic DAG runtime (T-302 / 1.3.0).
        this.logicService = new LogicServiceImpl(
                getLogger(), new LogicProgramStore(getLogger(), database.dataSource()));
        this.logicService.hydrate();

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

        // Item filter UI (T-300 / 1.1.0). Always-on now that the logistics solver is wired.
        this.uiService.register(new dev.brmz.sapientia.core.ui.FilterDescriptor(messages));

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
                    this, itemRegistry, overrideService, resourcePackBuilder, logicService);
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
                logisticsService.hydrateChunk(world.getName(), chunk.getX(), chunk.getZ());
                fluidsService.hydrateChunk(world.getName(), chunk.getX(), chunk.getZ());
            }
        }

        // Energy + logistics chunk hooks.
        getServer().getPluginManager().registerEvents(
                new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
                        energyService.hydrateChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                        logisticsService.hydrateChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                        fluidsService.hydrateChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                    }
                    @org.bukkit.event.EventHandler
                    public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
                        energyService.unloadChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                        logisticsService.unloadChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                        fluidsService.unloadChunk(
                                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
                    }
                }, this);

        // Energy tick — every 10 ticks (twice per second). Persists dirty nodes after each pass.
        getServer().getScheduler().runTaskTimer(this, () -> {
            energySolver.tick();
            energyService.persistDirty();
        }, 10L, 10L);

        // Logistics tick — every tick (T-300, P-004 envelope).
        getServer().getScheduler().runTaskTimer(this, () -> logisticsSolver.tick(), 5L, 1L);

        // Fluid tick — every 5 ticks (T-301, P-004 envelope; per-tank persistence batched).
        getServer().getScheduler().runTaskTimer(this, () -> {
            fluidsSolver.tick();
            fluidsService.persistDirty();
        }, 7L, 5L);

        // Logic tick — every 5 ticks (T-302, P-004 envelope; DAG worst-case is O(|V|+|E|)).
        getServer().getScheduler().runTaskTimer(this, () -> logicService.tickAll(), 9L, 5L);

        // Machine recipe-tick — every 10 ticks (T-404 / 1.4.1, mirrors energy cadence).
        getServer().getScheduler().runTaskTimer(this, () -> machineProcessor.tick(), 11L, 10L);

        // Petroleum / biochemistry kinetic loop — every 5 ticks (T-412..T-415 / 1.5.1).
        getServer().getScheduler().runTaskTimer(this, () -> petroleumTicker.tick(), 13L, 5L);

        // HV / electronics / gas kinetic loop — every 5 ticks (T-425 / T-426 / T-429 / 1.6.1).
        getServer().getScheduler().runTaskTimer(this, () -> electronicsTicker.tick(), 15L, 5L);

        // Geo & atmosphere kinetic loop — every 5 ticks (T-431..T-435 / 1.7.1).
        getServer().getScheduler().runTaskTimer(this, () -> geoTicker.tick(), 17L, 5L);

        // Advanced-logistics kinetic loop — every 10 ticks (T-450 / 1.8.1).
        getServer().getScheduler().runTaskTimer(this, () -> logisticsTicker.tick(), 19L, 10L);

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
