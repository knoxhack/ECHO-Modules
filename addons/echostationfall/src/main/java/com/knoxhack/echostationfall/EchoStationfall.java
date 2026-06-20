package com.knoxhack.echostationfall;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echostationfall.event.ModTooltipEvents;
import com.knoxhack.echostationfall.event.StationfallEvents;
import com.knoxhack.echostationfall.integration.StationfallCoreIntegration;
import com.knoxhack.echostationfall.integration.StationfallTerminalCommonIntegration;
import com.knoxhack.echostationfall.registry.*;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoStationfall.MODID)
public class EchoStationfall {
    public static final String MODID = "echostationfall";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITY_ATTRIBUTE_CREATION_EVENT =
            "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";

    public EchoStationfall(IEventBus modEventBus) {
        this((Object) modEventBus);
}

    EchoStationfall(Object modEventBus) {
        ModBlocks.register(modEventBus); ModEntities.register(modEventBus); ModItems.register(modEventBus);
        ModWorldgen.register(modEventBus); ModCreativeTabs.register(modEventBus);
        com.knoxhack.echostationfall.integration.prime.StationfallPrimeIntegration.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT,
                ModEntities::registerAttributes);
        StationfallEvents stationfallEvents = new StationfallEvents();
        ModTooltipEvents tooltipEvents = new ModTooltipEvents();
        EchoBackendLifecycleBridge.registerGameEventHandler(stationfallEvents::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(stationfallEvents::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(stationfallEvents::onClone);
        EchoBackendLifecycleBridge.registerGameEventHandler(tooltipEvents::onItemTooltip);
        Config.registerEchoConfig();
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echostationfall.EchoStationfallClient");
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
