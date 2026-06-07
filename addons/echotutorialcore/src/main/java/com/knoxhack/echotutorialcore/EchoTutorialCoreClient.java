package com.knoxhack.echotutorialcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public class EchoTutorialCoreClient {
    public EchoTutorialCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerGameEventHandler(com.knoxhack.echotutorialcore.client.TutorialToastOverlay::tick);
        EchoBackendLifecycleBridge.registerGameEventHandler(com.knoxhack.echotutorialcore.client.TutorialToastOverlay::render);
        EchoBackendLifecycleBridge.registerGameEventHandler(com.knoxhack.echotutorialcore.client.TutorialTooltipEvents::onItemTooltip);
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalNoticeSurface();
        }
    }

    private static void registerTerminalNoticeSurface() {
        try {
            Class.forName("com.knoxhack.echotutorialcore.integration.terminal.TutorialTerminalNoticeIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ClassNotFoundException exception) {
            EchoTutorialCore.LOGGER.debug("TutorialCore Terminal notice integration not present.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoTutorialCore.LOGGER.warn("TutorialCore Terminal notice integration could not be registered.", exception);
        }
    }
}
