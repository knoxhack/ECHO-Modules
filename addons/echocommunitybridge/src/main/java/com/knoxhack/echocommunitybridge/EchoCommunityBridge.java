package com.knoxhack.echocommunitybridge;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocommunitybridge.command.CommunityBridgeCommands;
import com.knoxhack.echocommunitybridge.discord.DiscordGatewayClient;
import com.knoxhack.echocommunitybridge.discord.DiscordMessageQueue;
import com.knoxhack.echocommunitybridge.integration.CommunityBridgeSignalOsIntegration;
import com.knoxhack.echocommunitybridge.server.BridgeEventHandler;
import com.knoxhack.echocommunitybridge.server.StatusHttpServer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoCommunityBridge {
    public static final String MODID = "echocommunitybridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoCommunityBridge(Object modEventBus, Object modContainer) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);

        EchoBackendLifecycleBridge.registerGameEventHandler(CommunityBridgeCommands::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(BridgeEventHandler::onServerStarted);
        EchoBackendLifecycleBridge.registerGameEventHandler(BridgeEventHandler::onServerStopping);
        EchoBackendLifecycleBridge.registerGameEventHandler(BridgeEventHandler::onPlayerLogin);
        EchoBackendLifecycleBridge.registerGameEventHandler(BridgeEventHandler::onPlayerLogout);
        EchoBackendLifecycleBridge.registerGameEventHandler(BridgeEventHandler::onServerChat);
        EchoBackendLifecycleBridge.registerGameEventHandler(BridgeEventHandler::onAdvancementEarned);
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO: Community Bridge online. Public status and Discord relay ready.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            if (EchoRuntimeModules.isLoaded("echosignalos") || EchoRuntimeModules.isLoaded("signalos")) {
                CommunityBridgeSignalOsIntegration.register();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                StatusHttpServer.INSTANCE.stop();
                DiscordGatewayClient.INSTANCE.shutdown();
                DiscordMessageQueue.INSTANCE.shutdown();
            }, "ECHO Community Bridge Shutdown"));
        });
    }
}
