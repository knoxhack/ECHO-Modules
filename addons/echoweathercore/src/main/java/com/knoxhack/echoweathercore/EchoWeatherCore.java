package com.knoxhack.echoweathercore;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoweathercore.client.config.WeatherCoreClientConfig;
import com.knoxhack.echoweathercore.command.WeatherCoreCommands;
import com.knoxhack.echoweathercore.config.WeatherCoreConfig;
import com.knoxhack.echoweathercore.data.WeatherDataReloadListener;
import com.knoxhack.echoweathercore.event.WeatherCoreEvents;
import com.knoxhack.echoweathercore.registry.WeatherCoreBlockEntities;
import com.knoxhack.echoweathercore.registry.WeatherCoreBlocks;
import com.knoxhack.echoweathercore.registry.WeatherCoreCreativeTabs;
import com.knoxhack.echoweathercore.registry.WeatherCoreItems;
import com.knoxhack.echoweathercore.registry.WeatherCoreMenus;
import com.knoxhack.echoweathercore.server.WeatherSleepHandler;
import com.knoxhack.echoweathercore.server.WeatherStateManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoWeatherCore.MODID)
public class EchoWeatherCore {
    public static final String MODID = "echoweathercore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoWeatherCore(IEventBus modEventBus) {
        this((Object) modEventBus);
    }

    EchoWeatherCore(Object modEventBus) {
        WeatherCoreItems.register(modEventBus);
        WeatherCoreBlocks.register(modEventBus);
        WeatherCoreBlockEntities.register(modEventBus);
        WeatherCoreMenus.register(modEventBus);
        WeatherCoreCreativeTabs.register(modEventBus);
        com.knoxhack.echoweathercore.integration.prime.WeatherCorePrimeIntegration.register();

        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onAddReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerStarting);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerStopping);
        EchoBackendLifecycleBridge.registerGameEventHandler(WeatherSleepHandler::onSleepFinished);
        WeatherCoreEvents.attach();

        EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoweathercore.EchoWeatherCoreClient");
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO: WeatherCore online. Atmospheric hazard framework initializing.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, EchoWeatherCore::registerOptionalIntegrations);
    }

    private void onRegisterCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            WeatherCoreCommands.register(dispatcher, EchoBackendCommandEventBridge.buildContext(event),
                    EchoBackendCommandEventBridge.commandSelection(event));
        }
    }

    private void onAddReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(event, id("weather_profiles"), WeatherDataReloadListener.INSTANCE);
    }

    private void onServerStarting(Object event) {
        var server = EchoBackendWorldEventBridge.serverStartingServer(event);
        if (server != null) {
            WeatherStateManager.getInstance().onServerStarting(server);
        }
    }

    private void onServerStopping(Object event) {
        if (EchoBackendWorldEventBridge.isServerStopping(event)) {
            WeatherStateManager.getInstance().onServerStopping();
        }
    }

    private static void registerOptionalIntegrations() {
        tryOptional("com.knoxhack.echoweathercore.integration.terminal.WeatherCoreTerminalIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.holomap.WeatherCoreHoloMapIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.lens.WeatherCoreLensIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.powergrid.WeatherCorePowerGridIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.soundcore.WeatherCoreSoundCoreIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.worldcore.WeatherCoreWorldCoreIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.mission.WeatherCoreMissionIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.nexus.WeatherCoreNexusIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.tutorial.WeatherCoreTutorialIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.drone.WeatherCoreDroneIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.faction.WeatherCoreFactionIntegration");
        tryOptional("com.knoxhack.echoweathercore.integration.runtimeguard.WeatherCoreRuntimeGuardIntegration");
    }

    private static void tryOptional(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Method m = clazz.getMethod("register");
            m.invoke(null);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Optional integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Optional integration {} could not be registered.", className, e);
        }
    }

    public static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, path);
    }
}
