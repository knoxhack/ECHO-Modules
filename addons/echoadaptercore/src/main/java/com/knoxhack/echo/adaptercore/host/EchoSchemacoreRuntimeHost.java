package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoschemacore} capabilities in the AdapterCore truth layer.
 */
public final class EchoSchemacoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoschemacore:runtime_host";
    private static final EchoSchemacoreRuntimeHost HOST = new EchoSchemacoreRuntimeHost();

    private EchoSchemacoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoschemacore.machine_use",
                "echoschemacore.machine_state_changed",
                "echoschemacore.mission_start",
                "echoschemacore.mission_complete",
                "echoschemacore.ui_open",
                "echoschemacore.ui_close",
                "echoschemacore.button_click",
                "echoschemacore.packet_send",
                "echoschemacore.packet_receive",
                "echoschemacore.region_enter",
                "echoschemacore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
