package com.knoxhack.echoashfallprotocol;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.knoxhack.echoashfallprotocol.command.CompanionDroneCommands;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneScanService;
import com.knoxhack.echoashfallprotocol.integration.AshfallCoreServices;
import com.knoxhack.echoashfallprotocol.integration.AshfallDroneLensIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallIndexProvider;
import com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallTerminalCommonIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallWorldCoreBuiltins;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.recipe.ScrapPressRecipe;
import com.knoxhack.echoashfallprotocol.registry.*;

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
public class EchoAshfallProtocol {
    public static final String MODID = "echoashfallprotocol";
    public static final Logger LOGGER = LogUtils.getLogger();
    private boolean missionCoreIntegrationRegistered;

    public EchoAshfallProtocol() {
        Config.registerEchoConfig();
        EchoCommandRegistry.register(CompanionDroneCommands.command());
        commonSetup();
    }

    private void commonSetup() {
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
