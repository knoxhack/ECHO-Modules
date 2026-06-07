package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoreportcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoReportcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoreportcore:runtime_host";
    private static final EchoReportcoreRuntimeHost HOST = new EchoReportcoreRuntimeHost();

    private EchoReportcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoreportcore.block_placed",
                "echoreportcore.block_broken",
                "echoreportcore.mission_start",
                "echoreportcore.mission_complete",
                "echoreportcore.packet_send",
                "echoreportcore.packet_receive",
                "echoreportcore.region_enter",
                "echoreportcore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
