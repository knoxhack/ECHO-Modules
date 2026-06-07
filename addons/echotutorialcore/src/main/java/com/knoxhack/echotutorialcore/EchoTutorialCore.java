package com.knoxhack.echotutorialcore;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echotutorialcore.command.TutorialCommands;
import com.knoxhack.echotutorialcore.data.ModAttachments;
import com.knoxhack.echotutorialcore.data.TutorialDataReloadListener;
import com.knoxhack.echotutorialcore.integration.TutorialCoreDiagnostics;
import com.knoxhack.echotutorialcore.integration.TutorialIntegrations;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import com.knoxhack.echotutorialcore.server.TutorialEventHandler;
import com.knoxhack.echotutorialcore.server.TutorialHintManager;
import com.mojang.logging.LogUtils;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoOptionalServices;
import org.slf4j.Logger;

public class EchoTutorialCore {
    public static final String MODID = "echotutorialcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoTutorialCore(Object modEventBus, Object modContainer) {
        ModAttachments.register(modEventBus);
        TutorialNetworking.register(modEventBus);

        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::registerServerReloadListener);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(TutorialEventHandler::onPlayerLogin);
        EchoBackendLifecycleBridge.registerGameEventHandler(TutorialEventHandler::onPlayerLogout);
        EchoBackendLifecycleBridge.registerGameEventHandler(TutorialEventHandler::onPlayerDeath);
        EchoBackendLifecycleBridge.registerGameEventHandler(TutorialEventHandler::onBlockPlace);
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO: TutorialCore online. Ashfall deep, but not confusing.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoCoreServices.registerDiagnosticService(TutorialCoreDiagnostics::diagnostics);
            TutorialIntegrations.registerOptionalIntegrations();
        });
    }

    private void registerServerReloadListener(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(
                event,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "tutorial_data"),
                new TutorialDataReloadListener());
    }

    private void onRegisterCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            TutorialCommands.register(dispatcher, null, null);
        }
    }

    private void onServerTick(Object event) {
        net.minecraft.server.MinecraftServer server = EchoBackendWorldEventBridge.serverTickServer(event);
        if (server == null) {
            return;
        }
        // Evaluate contextual hints periodically (every 5 seconds approx)
        long gameTime = server.overworld().getGameTime();
        if (gameTime % 100 == 0) {
            if (EchoOptionalServices.runtimeGuardOrNoOp().isOverBudget("tutorialcore")) {
                LOGGER.debug("Skipping TutorialCore hint evaluation while RuntimeGuard reports tutorialcore over budget.");
                return;
            }
            for (var player : server.getPlayerList().getPlayers()) {
                TutorialHintManager.evaluateHints(player);
            }
        }
    }

}
