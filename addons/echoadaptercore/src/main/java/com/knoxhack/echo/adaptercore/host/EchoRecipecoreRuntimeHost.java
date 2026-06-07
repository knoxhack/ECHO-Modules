package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echorecipecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoRecipecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echorecipecore:runtime_host";
    private static final EchoRecipecoreRuntimeHost HOST = new EchoRecipecoreRuntimeHost();

    private EchoRecipecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echorecipecore.machine_use",
                "echorecipecore.machine_state_changed",
                "echorecipecore.region_enter",
                "echorecipecore.discovery"),
                Set.of(),
                true,
                false,
                false));

        try {
            Class<?> handlerClass = Class.forName("com.knoxhack.echorecipecore.integration.RecipecoreActionHandler");
            handlerClass.getMethod("register").invoke(null);
        } catch (Exception ignored) {
            // Addon action handler not present
        }
    }
}
