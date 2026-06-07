package com.knoxhack.echoprimecore;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoprimecore.command.PrimeCommands;
import com.knoxhack.echoprimecore.config.PrimeConfig;
import com.knoxhack.echoprimecore.integration.PrimeIntegrationLoader;
import com.knoxhack.echoprimecore.progression.PrimeFirstJoinHandler;
import com.knoxhack.echoprimecore.progression.PrimeStarterFlow;
import com.knoxhack.echoprimecore.registry.ModAttachments;
import com.knoxhack.echoprimecore.registry.ModBlocks;
import com.knoxhack.echoprimecore.registry.ModCreativeTabs;
import com.knoxhack.echoprimecore.registry.ModEntities;
import com.knoxhack.echoprimecore.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class EchoPrimeCore {
    public static final String MODID = "echoprimecore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoPrimeCore(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        PrimeConfig.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(PrimeFirstJoinHandler::onPlayerLoggedIn);
        EchoBackendLifecycleBridge.registerGameEventHandler(PrimeStarterFlow::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(PrimeStarterFlow::onItemCrafted);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::registerCommands);
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO: Prime Core online. Stable world, low signal, survival-first.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, PrimeIntegrationLoader::registerAll);
    }

    private void registerCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            PrimeCommands.register(dispatcher);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
