package com.knoxhack.echopresencelink.client;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echopresencelink.EchoPresenceLink;

public final class EchoPresenceClientBootstrap {
    private EchoPresenceClientBootstrap() {
    }

    public static void register(Object modEventBus, Object modContainer) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoPresenceClientBootstrap::clientSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoPresenceClientBootstrap::clientTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(PresenceClientCommands::register);
    }

    private static void clientSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> Runtime.getRuntime().addShutdownHook(new Thread(
                PresenceController.INSTANCE::shutdown,
                "ECHO Presence Link Shutdown")));
        EchoPresenceLink.LOGGER.info("ECHO Presence Link client IPC controller online.");
    }

    private static void clientTick(Object event) {
        PresenceController.INSTANCE.tick();
    }
}
