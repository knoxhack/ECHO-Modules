package com.knoxhack.echostationfall;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echostationfall.event.ModTooltipEvents;
import com.knoxhack.echostationfall.event.StationfallEvents;
import com.knoxhack.echostationfall.integration.StationfallCoreIntegration;
import com.knoxhack.echostationfall.integration.StationfallTerminalCommonIntegration;
import com.knoxhack.echostationfall.registry.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoStationfall {
    public static final String MODID = "echostationfall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoStationfall(Object modEventBus) {
        ModBlocks.register(modEventBus); ModEntities.register(modEventBus); ModItems.register(modEventBus);
        ModWorldgen.register(modEventBus); ModCreativeTabs.register(modEventBus);
        com.knoxhack.echostationfall.integration.prime.StationfallPrimeIntegration.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
        StationfallEvents stationfallEvents = new StationfallEvents();
        ModTooltipEvents tooltipEvents = new ModTooltipEvents();
        EchoBackendLifecycleBridge.registerGameEventHandler(stationfallEvents::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(stationfallEvents::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(stationfallEvents::onClone);
        EchoBackendLifecycleBridge.registerGameEventHandler(tooltipEvents::onItemTooltip);
        Config.registerEchoConfig();
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            StationfallCoreIntegration.register();
            if (EchoRuntimeModules.isLoaded("echoterminal")) {
                StationfallTerminalCommonIntegration.register();
            }
        });
    }
}
