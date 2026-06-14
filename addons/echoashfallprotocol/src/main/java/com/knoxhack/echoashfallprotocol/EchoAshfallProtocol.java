package com.knoxhack.echoashfallprotocol;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.knoxhack.echoashfallprotocol.command.CompanionDroneCommands;
import com.knoxhack.echoashfallprotocol.data.SaveMigrationHandler;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneScanService;
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
    private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";
    public static final Logger LOGGER = LogUtils.getLogger();
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
        EchoCommandRegistry.register(CompanionDroneCommands.command());
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_LOGGED_IN_EVENT, SaveMigrationHandler::onPlayerLogin);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
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
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            LOGGER.info("=== ECHO: Ashfall Protocol ===");
            LOGGER.info("Initializing survival systems...");
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
            registerScrapPressRecipes();
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
        });
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

    private void registerMachineCoreRuntimeProvider() {
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

    private void registerScrapPressRecipes() {
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
