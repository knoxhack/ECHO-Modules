package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoguidecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoGuidecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoguidecore:runtime_host";
    private static final EchoGuidecoreRuntimeHost HOST = new EchoGuidecoreRuntimeHost();

    private EchoGuidecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoguidecore.machine_use",
                "echoguidecore.machine_state_changed",
                "echoguidecore.mission_start",
                "echoguidecore.mission_complete",
                "echoguidecore.ui_open",
                "echoguidecore.ui_close",
                "echoguidecore.button_click",
                "echoguidecore.region_enter",
                "echoguidecore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
