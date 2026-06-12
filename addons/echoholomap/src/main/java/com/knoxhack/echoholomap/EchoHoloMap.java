package com.knoxhack.echoholomap;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoholomap.command.HoloMapCommands;
import com.knoxhack.echoholomap.integration.HoloMapIndexIntegration;
import com.knoxhack.echoholomap.integration.HoloMapMissionCoreIntegration;
import com.knoxhack.echoholomap.map.BuiltinHoloMapChunkActionProvider;
import com.knoxhack.echoholomap.map.BuiltinHoloMapRouteHazardProvider;
import com.knoxhack.echoholomap.map.HoloMapChunkActions;
import com.knoxhack.echoholomap.map.HoloMapService;
import com.knoxhack.echoholomap.map.HoloMapTerrainScanner;
import com.knoxhack.echoholomap.network.ModNetwork;
import com.knoxhack.echoholomap.world.HoloMapDeathpointEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public final class EchoHoloMap {
    public static final String MODID = "echoholomap";
    public static final String CHAPTER_ID = "holomap";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoHoloMap(Object modEventBus, Object modContainer) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        Config.registerEchoConfig();
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapCommands::register);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapTerrainScanner::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapDeathpointEvents::onPlayerDeath);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapDeathpointEvents::onPlayerLoggedIn);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapDeathpointEvents::onPlayerRespawn);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            HoloMapService.INSTANCE.registerBuiltins();
            HoloMapService.INSTANCE.registerHoloProvider(BuiltinHoloMapRouteHazardProvider.INSTANCE);
            HoloMapChunkActions.register(BuiltinHoloMapChunkActionProvider.INSTANCE);
            EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
            registerAddonChapter();
            if (modulePresent("com.knoxhack.echomissioncore.EchoMissionCore")) {
                HoloMapMissionCoreIntegration.register();
            }
            if (modulePresent("com.knoxhack.echomachinecore.EchoMachineCore")) {
                registerMachineCoreIntegration();
            }
            if (modulePresent("com.knoxhack.echoterminal.EchoTerminal")) {
                registerTerminalIntegration();
            }
            if (modulePresent("com.knoxhack.echoindex.EchoIndex")) {
                HoloMapIndexIntegration.register();
            }
            LOGGER.info("ECHO: HoloMap online. {}", EchoCoreServices.platformProviderSummary());
        });
    }

    private static void registerAddonChapter() {
        if (EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
            return;
        }
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return CHAPTER_ID;
            }

            @Override
            public String modId() {
                return MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: HoloMap";
            }

            @Override
            public String summary() {
                return "Terminal-integrated world telemetry, route, scan, and marker command map.";
            }

            @Override
            public String statusLine(Player player) {
                int layers = EchoCoreServices.mapLayers(player).size();
                int markers = EchoCoreServices.mapMarkers(player).size();
                return "HoloMap: " + layers + " layer(s), " + markers + " marker(s), "
                        + EchoCoreServices.mapMarkerService().providerCount() + " provider(s).";
            }
        });
    }

    private static void registerTerminalIntegration() {
        try {
            Class.forName("com.knoxhack.echoholomap.integration.HoloMapTerminalCommonIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("ECHO HoloMap terminal integration could not be registered.", exception);
        }
    }

    private static void registerMachineCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoholomap.integration.MachineCoreHoloMapIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO HoloMap MachineCore integration could not be registered.", exception);
        }
    }

    private static boolean modulePresent(String className) {
        try {
            Class.forName(className, false, EchoHoloMap.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}
