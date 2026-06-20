package com.knoxhack.echoholomap;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
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
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

@Mod(EchoHoloMap.MODID)
public final class EchoHoloMap {
    public static final String MODID = "echoholomap";
    public static final String CHAPTER_ID = "holomap";
    private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);

    public EchoHoloMap(IEventBus modEventBus, ModContainer modContainer) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_PAYLOAD_HANDLERS_EVENT,
                ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        Config.registerEchoConfig();
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapCommands::register);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapTerrainScanner::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapDeathpointEvents::onPlayerDeath);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapDeathpointEvents::onPlayerLoggedIn);
        EchoBackendLifecycleBridge.registerGameEventHandler(HoloMapDeathpointEvents::onPlayerRespawn);
        registerOptionalGameTests(modEventBus, "com.knoxhack.echoholomap.test.ModGameTests");

        EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoholomap.EchoHoloMapClient");
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> registerCommonServices("neoforge_common_setup"));
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader() {
        return registerCommonServices("native_loader_module_ready");
    }

    private static boolean registerCommonServices(String source) {
        if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        HoloMapService.INSTANCE.registerBuiltins();
        HoloMapService.INSTANCE.registerHoloProvider(BuiltinHoloMapRouteHazardProvider.INSTANCE);
        HoloMapChunkActions.register(BuiltinHoloMapChunkActionProvider.INSTANCE);
        EchoCoreServices.registerMapMarkerService(HoloMapService.INSTANCE);
        registerAddonChapter();
        if (modulePresent("echomissioncore", "com.knoxhack.echomissioncore.EchoMissionCore")) {
            HoloMapMissionCoreIntegration.register();
        }
        if (modulePresent("echomachinecore", "com.knoxhack.echomachinecore.EchoMachineCore")) {
            registerMachineCoreIntegration();
        }
        if (modulePresent("echoterminal", "com.knoxhack.echoterminal.EchoTerminal")) {
            registerTerminalIntegration();
        }
        if (modulePresent("echoindex", "com.knoxhack.echoindex.EchoIndex")) {
            HoloMapIndexIntegration.register();
        }
        LOGGER.info("ECHO: HoloMap online [{}]. {}", source, EchoCoreServices.platformProviderSummary());
        return true;
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

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO HoloMap GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO HoloMap GameTest instances could not be registered.", exception);
        }
    }

    private static boolean modulePresent(String moduleId, String className) {
        if (EchoRuntimeModules.isLoaded(moduleId)) {
            return true;
        }
        try {
            Class.forName(className, false, EchoHoloMap.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}
