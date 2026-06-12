package com.knoxhack.echobasegrid;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echobasegrid.config.BaseGridConfig;
import com.knoxhack.echobasegrid.command.BaseGridCommands;
import com.knoxhack.echobasegrid.network.BaseGridNetwork;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoBaseGrid {
    public static final String MODID = "echobasegrid";
    public static final Logger LOGGER = LoggerFactory.getLogger(EchoBaseGrid.class);

    public EchoBaseGrid(Object modEventBus) {
        var runtime = Agent9BaseGridRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO: Base Grid Agent 9 native host adapter {}.", runtime.get("status"));
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        com.knoxhack.echobasegrid.integration.prime.BaseGridPrimeIntegration.register();
        BaseGridConfig.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, BaseGridNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerGameEventHandler(BaseGridCommands::onRegisterCommands);
        LOGGER.info("ECHO: Base Grid online.");
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, EchoBaseGrid::registerHoloMapIntegration);
    }

    private static void registerHoloMapIntegration() {
        if (!EchoRuntimeModules.isLoaded("echoholomap")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echobasegrid.integration.holomap.BaseGridHoloMapIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            LOGGER.warn("ECHO: Base Grid HoloMap integration could not be registered.", exception);
        }
    }

    public static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, path);
    }

}
