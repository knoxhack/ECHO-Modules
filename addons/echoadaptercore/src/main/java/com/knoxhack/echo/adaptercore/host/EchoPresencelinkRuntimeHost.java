package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echopresencelink} capabilities in the AdapterCore truth layer.
 */
public final class EchoPresencelinkRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echopresencelink:runtime_host";
    private static final EchoPresencelinkRuntimeHost HOST = new EchoPresencelinkRuntimeHost();

    private EchoPresencelinkRuntimeHost() {
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
                "echopresencelink.mission_start",
                "echopresencelink.mission_complete",
                "echopresencelink.ui_open",
                "echopresencelink.ui_close",
                "echopresencelink.button_click",
                "echopresencelink.packet_send",
                "echopresencelink.packet_receive",
                "echopresencelink.region_enter",
                "echopresencelink.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
