package com.knoxhack.echoashfallprotocol;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.knoxhack.echoashfallprotocol.command.CompanionDroneCommands;
import com.knoxhack.echoashfallprotocol.data.SaveMigrationHandler;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneScanService;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreMissionTriggerRuntime;
import com.knoxhack.echoashfallprotocol.event.CompanionDroneEvents;
import com.knoxhack.echoashfallprotocol.event.PlayerStartingKitHandler;
import com.knoxhack.echoashfallprotocol.integration.AshfallCoreServices;
import com.knoxhack.echoashfallprotocol.integration.AshfallDroneLensIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallIndexProvider;
import com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallTerminalCommonIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallWorldCoreBuiltins;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.network.ModNetwork;
import com.knoxhack.echoashfallprotocol.recipe.ScrapPressRecipe;
import com.knoxhack.echoashfallprotocol.registry.*;
import com.knoxhack.echoashfallprotocol.survival.PlayerTechTracker;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * ECHO: Ashfall Protocol
 * A post-apocalyptic survival overhaul for Minecraft.
 * 
 * Core systems:
 * - ECHO-7 AI Guide
 * - Friction-based progression
 * - Mutation system
 * - Smart reactive events
 * - Deep machine crafting
 */
@Mod(EchoAshfallProtocol.MODID)
public class EchoAshfallProtocol {
    public static final String MODID = "echoashfallprotocol";
    private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String PLAYER_LOGGED_IN_EVENT = "net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent";
    private static final String PLAYER_TICK_POST_EVENT = "net.neoforged.neoforge.event.tick.PlayerTickEvent$Post";
    private static final String ITEM_ENTITY_PICKUP_POST_EVENT =
            "net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent$Post";
    private static final String REGISTER_COMMANDS_EVENT = "net.neoforged.neoforge.event.RegisterCommandsEvent";
    private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean RUNTIME_EVENT_HANDLERS_REGISTERED = new AtomicBoolean(false);
    private boolean missionCoreIntegrationRegistered;

    public EchoAshfallProtocol(IEventBus modEventBus) {
        Config.registerEchoConfig();
        registerDeferredContent(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_PAYLOAD_HANDLERS_EVENT, ModNetwork::register);
        modEventBus.addListener((EntityAttributeCreationEvent event) -> ModEntities.registerAttributes(event));
        modEventBus.addListener((RegisterSpawnPlacementsEvent event) -> ModEntities.registerSpawnPlacements(event));
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {
            ModEnergyCapabilities.register(event);
            ModItemCapabilities.register(event);
        });
        registerOptionalGameTests(modEventBus, "com.knoxhack.echoashfallprotocol.test.ModGameTests");
        registerRuntimeEventHandlers();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
            "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolClient");
}

    private static void onRegisterCommands(Object event) {
        if (!isEvent(event, REGISTER_COMMANDS_EVENT)) {
            return;
        }
        AshfallAdapterCoreMissionTriggerRuntime.onRegisterCommands(event);
    }

    private static void onPlayerLoggedIn(Object event) {
        if (!isEvent(event, PLAYER_LOGGED_IN_EVENT)) {
            return;
        }
        runEventStep("save_migration", SaveMigrationHandler::onPlayerLogin, event);
        runEventStep("missioncore_first_route", AshfallAdapterCoreMissionTriggerRuntime::onPlayerLoggedIn, event);
        runEventStep("first_spawn_runtime", PlayerStartingKitHandler::onPlayerLoggedIn, event);
        runEventStep("companion_drone", CompanionDroneEvents::onPlayerLoggedIn, event);
        runEventStep("player_tech_tracker", PlayerTechTracker::onPlayerLoggedIn, event);
    }

    private static void onPlayerTickPost(Object event) {
        if (!isEvent(event, PLAYER_TICK_POST_EVENT)) {
            return;
        }
        AshfallAdapterCoreMissionTriggerRuntime.onPlayerTick(event);
        CompanionDroneEvents.onPlayerTick(event);
    }

    private static void onItemEntityPickupPost(Object event) {
        if (!isEvent(event, ITEM_ENTITY_PICKUP_POST_EVENT)) {
            return;
        }
        AshfallAdapterCoreMissionTriggerRuntime.onItemObtained(event);
    }

    private static boolean isEvent(Object event, String eventClassName) {
        return event != null && event.getClass().getName().equals(eventClassName);
    }

    private static void runEventStep(
            String step,
            java.util.function.Consumer<Object> handler,
            Object event) {
        try {
            handler.accept(event);
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.warn("Ashfall event step {} failed; continuing remaining event hooks.", step, exception);
        }
    }

    private void registerDeferredContent(Object modEventBus) {
        ModDataComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModEffects.register(modEventBus);
        ModSounds.register(modEventBus);
        ModPoiTypes.register(modEventBus);
        ModBiomes.register(modEventBus);
        ModLootModifiers.register(modEventBus);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> registerCommonServices("neoforge_common_setup"));
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader() {
        registerRuntimeEventHandlers();
        return registerCommonServices("native_loader_module_ready");
    }

    private static boolean registerRuntimeEventHandlers() {
        if (!gameEventBusAvailable()) {
            LOGGER.debug("Ashfall runtime event handler registration deferred because the game event bus is unavailable.");
            return false;
        }
        if (!RUNTIME_EVENT_HANDLERS_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        EchoCommandRegistry.register(CompanionDroneCommands.command());
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoCommandRegistry::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_COMMANDS_EVENT,
                EchoAshfallProtocol::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_LOGGED_IN_EVENT, EchoAshfallProtocol::onPlayerLoggedIn);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_TICK_POST_EVENT, EchoAshfallProtocol::onPlayerTickPost);
        EchoBackendLifecycleBridge.registerGameEventHandler(ITEM_ENTITY_PICKUP_POST_EVENT,
                EchoAshfallProtocol::onItemEntityPickupPost);
        LOGGER.info("Ashfall runtime event handlers registered for command, login, tick, and item pickup events.");
        return true;
    }

    private static boolean gameEventBusAvailable() {
        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            return neoForge.getField("EVENT_BUS").get(null) != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static boolean registerCommonServices(String source) {
        if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        LOGGER.info("=== ECHO: Ashfall Protocol ===");
        LOGGER.info("Initializing survival systems [{}]...", source);
        LOGGER.info("Loading Ashfall + Orbital public beta route v1.3.0...");

        AshfallCoreServices.register();
        AshfallAdapterCoreMachineRuntimeHost.register();
        if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            registerMachineCoreRuntimeProvider();
        }
        CompanionDroneStateStore.registerDataKey();
        DroneScanService.registerMapProvider();
        if (EchoRuntimeModules.isLoaded("echoworldcore")) {
            AshfallWorldCoreBuiltins.register();
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            AshfallTerminalCommonIntegration.register();
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            AshfallDroneLensIntegration.register();
        }
        registerScrapPressRecipes(source);
        if (EchoRuntimeModules.isLoaded("echoindex")) {
            AshfallIndexProvider.register();
        }

        LOGGER.info("ECHO-7 AI Guide: ONLINE");
        LOGGER.info("Mutation System: ACTIVE");
        LOGGER.info("Smart Event Framework: ENABLED");
        LOGGER.info("Faction System: ACTIVE (3 Echo Core Ashfall factions)");
        LOGGER.info("Research System: ACTIVE (15 Perks, 5 Schematics)");
        LOGGER.info("Cold Survival: ACTIVE (Cryogenic Ruins Biome)");
        LOGGER.info("Fast Travel: ACTIVE (Radio Network)");
        LOGGER.info("POI System: ACTIVE (route-specific exploration profiles)");
        LOGGER.info("All systems initialized. Welcome to the wasteland.");
        return true;
    }

    public void onServerStarted() {
        tryRegisterMissionCoreIntegration();
    }

    public void onServerTickPost() {
        tryRegisterMissionCoreIntegration();
    }

    private void tryRegisterMissionCoreIntegration() {
        if (missionCoreIntegrationRegistered) {
            return;
        }
        missionCoreIntegrationRegistered = AshfallMissionCoreIntegration.registerWhenReady();
    }

    private static void registerMachineCoreRuntimeProvider() {
        try {
            Class.forName("com.knoxhack.echoashfallprotocol.integration.AshfallMachineCoreRuntimeProvider")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Ashfall MachineCore runtime provider is unavailable in this addon profile.", exception);
        }
    }

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Ashfall GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Ashfall GameTest instances could not be registered.", exception);
        }
    }

    private static void registerScrapPressRecipes(String source) {
        if (EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive()) {
            LOGGER.info("Ashfall Scrap Press live ItemStack recipes deferred for Native Loader [{}]; "
                    + "native machine-power contracts expose the logical recipe surface.", source);
            return;
        }
        // Use lazy suppliers to avoid creating ItemStacks during mod setup
        // (ItemStack requires components to be bound, which happens later)

        // 9 Scrap Metal -> 1 Machine Casing (compressed crafting component)
        ScrapPressRecipe.register(
            ModItems.SCRAP_METAL, 9,
            ModItems.MACHINE_CASING, 1,
            40
        );

        // 4 Scrap Circuits -> 1 Circuit Board (salvage usable components)
        ScrapPressRecipe.register(
            ModItems.SCRAP_CIRCUIT, 4,
            ModItems.CIRCUIT_BOARD, 1,
            60
        );

        // 4 Scrap Plastic -> 1 Filtration Membrane (pressure-forming)
        ScrapPressRecipe.register(
            ModItems.SCRAP_PLASTIC, 4,
            ModItems.FILTRATION_MEMBRANE, 1,
            50
        );

        LOGGER.info("Registered {} Scrap Press recipes", ScrapPressRecipe.getAllRecipes().size());
    }
}
