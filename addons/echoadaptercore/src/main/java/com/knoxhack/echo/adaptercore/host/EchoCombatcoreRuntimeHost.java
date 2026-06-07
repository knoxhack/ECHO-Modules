package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echocombatcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoCombatcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocombatcore:runtime_host";
    private static final EchoCombatcoreRuntimeHost HOST = new EchoCombatcoreRuntimeHost();

    private EchoCombatcoreRuntimeHost() {
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
                "echocombatcore.region_enter",
                "echocombatcore.discovery"),
                Set.of(),
                true,
                false,
                false));

        try {
            Class<?> handlerClass = Class.forName("com.knoxhack.echocombatcore.integration.CombatcoreActionHandler");
            handlerClass.getMethod("register").invoke(null);
        } catch (Exception ignored) {
            // Addon action handler not present
        }
    }
}
