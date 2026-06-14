package com.knoxhack.echopresencelink;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echopresencelink.api.EchoPresenceRegistry;
import com.knoxhack.echopresencelink.integration.PresenceLinkSignalOsIntegration;
import com.knoxhack.echopresencelink.presence.CoreEchoPresenceProvider;
import com.knoxhack.echopresencelink.presence.PresenceCoreDiagnostics;
import com.knoxhack.echopresencelink.presence.TerminalPresenceProvider;
import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationTargetException;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoPresenceLink.MODID)
public class EchoPresenceLink {
    public static final String MODID = "echopresencelink";
    public static final Logger LOGGER = LogUtils.getLogger();

    EchoPresenceLink() {
        this(null, null);
    }

    public EchoPresenceLink(IEventBus modEventBus, ModContainer modContainer) {
        EchoPresenceRegistry.register(new CoreEchoPresenceProvider());
        EchoPresenceRegistry.register(new TerminalPresenceProvider());
        EchoCoreServices.registerDiagnosticService(new PresenceCoreDiagnostics());

        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
                "com.knoxhack.echopresencelink.test.ModGameTests");
        registerClientBootstrap(modEventBus, modContainer);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            if (EchoRuntimeModules.isLoaded("echosignalos") || EchoRuntimeModules.isLoaded("signalos")) {
                PresenceLinkSignalOsIntegration.register();
            }
        });
        LOGGER.info("ECHO: Presence Link registered. Discord activity publishing is client-side only.");
    }

    private static void registerClientBootstrap(Object modEventBus, Object modContainer) {
        try {
            Class.forName("com.knoxhack.echopresencelink.client.EchoPresenceClientBootstrap")
                    .getMethod("register", Object.class, Object.class)
                    .invoke(null, modEventBus, modContainer);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            LOGGER.warn("ECHO Presence Link client bootstrap is unavailable.", exception);
        } catch (InvocationTargetException exception) {
            LOGGER.warn("ECHO Presence Link client bootstrap failed.", exception.getCause());
        } catch (LinkageError error) {
            LOGGER.warn("ECHO Presence Link client bootstrap could not link.", error);
        }
    }
}
