package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echometadatacore} capabilities in the AdapterCore truth layer.
 */
public final class EchoMetadatacoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echometadatacore:runtime_host";
    private static final EchoMetadatacoreRuntimeHost HOST = new EchoMetadatacoreRuntimeHost();

    private EchoMetadatacoreRuntimeHost() {
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
                "echometadatacore.mission_start",
                "echometadatacore.mission_complete",
                "echometadatacore.ui_open",
                "echometadatacore.ui_close",
                "echometadatacore.button_click",
                "echometadatacore.packet_send",
                "echometadatacore.packet_receive",
                "echometadatacore.region_enter",
                "echometadatacore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
