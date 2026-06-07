package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoplayercore} capabilities in the AdapterCore truth layer.
 */
public final class EchoPlayercoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoplayercore:runtime_host";
    private static final EchoPlayercoreRuntimeHost HOST = new EchoPlayercoreRuntimeHost();

    private EchoPlayercoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echoplayercore.block_placed",
                "echoplayercore.block_broken",
                "echoplayercore.mission_start",
                "echoplayercore.mission_complete",
                "echoplayercore.save_write",
                "echoplayercore.save_read",
                "echoplayercore.packet_send",
                "echoplayercore.packet_receive",
                "echoplayercore.region_enter",
                "echoplayercore.discovery"),
                Set.of(),
                true,
                true,
                false));
    }
}
