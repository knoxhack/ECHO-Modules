package com.knoxhack.echo.npcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.npcore.command.EchoNpcCoreCommands;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.conversion.EchoNpcReplacementService;
import com.knoxhack.echo.npcore.data.EchoNpcReloaders;
import com.knoxhack.echo.npcore.diagnostics.EchoNpcCoreDiagnostics;
import com.knoxhack.echo.npcore.integration.EchoNpcCoreOptionalIntegrations;
import com.knoxhack.echo.npcore.network.ModNetwork;
import com.knoxhack.echo.npcore.registry.ModCreativeTabs;
import com.knoxhack.echo.npcore.registry.ModEntities;
import com.knoxhack.echo.npcore.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoNpcCore {
    public static final String MODID = "echonpcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("ECHO NPCore");

    public EchoNpcCore(Object modEventBus) {
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        com.knoxhack.echo.npcore.integration.prime.NpcCorePrimeIntegration.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::register);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);

        EchoBackendLifecycleBridge.registerGameEventHandler(EchoNpcReloaders::addServerReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoNpcCoreCommands::register);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoNpcReplacementService::onEntityJoinLevel);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoNpcReplacementService::onEntityInteract);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoNpcCoreDiagnostics.logStartup();
            EchoNpcCoreOptionalIntegrations.registerCommon();
        });
    }
}
