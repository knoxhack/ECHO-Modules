package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echosocialcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoSocialcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echosocialcore:runtime_host";
    private static final EchoSocialcoreRuntimeHost HOST = new EchoSocialcoreRuntimeHost();

    private EchoSocialcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echosocialcore.mission_start",
                "echosocialcore.mission_complete",
                "echosocialcore.ui_open",
                "echosocialcore.ui_close",
                "echosocialcore.button_click",
                "echosocialcore.region_enter",
                "echosocialcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
