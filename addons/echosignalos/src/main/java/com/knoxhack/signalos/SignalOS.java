package com.knoxhack.signalos;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.signalos.content.SignalOsBuiltinContent;
import com.knoxhack.signalos.content.SignalOsServerReloaders;
import com.knoxhack.signalos.registry.ModBlockEntities;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.registry.ModCreativeTabs;
import com.knoxhack.signalos.registry.ModDataComponents;
import com.knoxhack.signalos.registry.ModMenus;
import com.knoxhack.signalos.integration.SignalOsMissionCoreIntegration;
import com.knoxhack.signalos.service.SignalOsBuiltinActions;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import com.knoxhack.signalos.network.ModNetwork;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class SignalOS {
    public static final String MODID = "signalos";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SignalOS(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(SignalOsServerReloaders::addServerReloadListeners);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            SignalOsBuiltinActions.register();
            SignalOsBuiltinContent.register();
            SignalOsTerminalServices.registerEchoCoreServices();
            SignalOsMissionCoreIntegration.register();
        });
        LOGGER.info("SignalOS computer OS online.");
    }
}
