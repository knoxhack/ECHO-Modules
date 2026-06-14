package com.knoxhack.echoworldcore;

import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoworldcore.event.WorldCoreEvents;
import com.knoxhack.echoworldcore.integration.WorldCoreDiagnosticProvider;
import com.knoxhack.echoworldcore.integration.WorldCoreDiscoveryProvider;
import com.knoxhack.echoworldcore.integration.WorldCoreIndexProvider;
import com.knoxhack.echoworldcore.integration.WorldCoreMapDataProvider;
import com.knoxhack.echoworldcore.registry.WorldCoreBuiltins;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

@Mod(EchoWorldCore.MODID)
public final class EchoWorldCore {
    public static final String MODID = "echoworldcore";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String CHAPTER_ID = "world_core";

    public EchoWorldCore(IEventBus modEventBus) {
        Config.registerEchoConfig();
        EchoCoreServices.registerWorldRegionService(WorldRegionService.INSTANCE);
        WorldCoreEvents.attach();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        registerOptionalGameTests(modEventBus, "com.knoxhack.echoworldcore.test.ModGameTests");
    }

    public void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            WorldRegionService service = WorldRegionService.INSTANCE;
            WorldCoreBuiltins.register(service);
            EchoCoreServices.registerDiscoveryProvider(new WorldCoreDiscoveryProvider(service));
            EchoCoreServices.registerDiagnosticService(WorldCoreDiagnosticProvider.INSTANCE);
            EchoCoreServices.registerIndexContentProvider(WorldCoreIndexProvider.INSTANCE);
            EchoCoreServices.registerMapDataProvider(WorldCoreMapDataProvider.INSTANCE);
            registerAddonChapter();
            if (EchoRuntimeModules.isLoaded("echoterminal")) {
                registerTerminalIntegration();
            }
            if (EchoRuntimeModules.isLoaded("echoholomap")) {
                registerHoloMapIntegration();
            }
            LOGGER.info("ECHO WorldCore initialized with {} region definitions and {} hazard definitions.",
                    service.regionDefinitions().size(), service.hazardDefinitions().size());
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
                return "ECHO: WorldCore";
            }

            @Override
            public String summary() {
                return "Shared world regions, hazards, markers, structure discovery, and runtime world events.";
            }

            @Override
            public String statusLine(Player player) {
                WorldRegionService service = WorldRegionService.INSTANCE;
                if (player == null) {
                    return "WorldCore: " + service.regionDefinitions().size() + " shared region definitions online.";
                }
                return "WorldCore: " + service.activeRegions(player).size() + " active region(s), "
                        + service.markers(player).size() + " known marker(s).";
            }
        });
    }

    private static void registerTerminalIntegration() {
        try {
            Class.forName("com.knoxhack.echoworldcore.integration.WorldCoreTerminalIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("WorldCore terminal integration could not be registered.", exception);
        }
    }

    private static void registerHoloMapIntegration() {
        try {
            Class.forName("com.knoxhack.echoworldcore.integration.WorldCoreHoloMapRichProvider")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("WorldCore HoloMap rich zone integration could not be registered.", exception);
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
            LOGGER.warn("ECHO WorldCore GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO WorldCore GameTest instances could not be registered.", exception);
        }
    }
}
