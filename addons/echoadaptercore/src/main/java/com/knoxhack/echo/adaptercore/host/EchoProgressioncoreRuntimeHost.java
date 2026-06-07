package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoprogressioncore} capabilities in the AdapterCore truth layer.
 */
public final class EchoProgressioncoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoprogressioncore:runtime_host";
    private static final EchoProgressioncoreRuntimeHost HOST = new EchoProgressioncoreRuntimeHost();

    private EchoProgressioncoreRuntimeHost() {
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
                "echoprogressioncore.machine_use",
                "echoprogressioncore.machine_state_changed",
                "echoprogressioncore.mission_start",
                "echoprogressioncore.mission_complete",
                "echoprogressioncore.ui_open",
                "echoprogressioncore.ui_close",
                "echoprogressioncore.button_click",
                "echoprogressioncore.region_enter",
                "echoprogressioncore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
