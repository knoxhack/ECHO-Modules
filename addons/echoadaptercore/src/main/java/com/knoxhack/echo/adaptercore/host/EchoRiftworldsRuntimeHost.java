package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoriftworlds} capabilities in the AdapterCore truth layer.
 */
public final class EchoRiftworldsRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoriftworlds:runtime_host";
    private static final EchoRiftworldsRuntimeHost HOST = new EchoRiftworldsRuntimeHost();

    private EchoRiftworldsRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoriftworlds.item_crafted",
                "echoriftworlds.item_used",
                "echoriftworlds.block_placed",
                "echoriftworlds.block_broken",
                "echoriftworlds.mission_start",
                "echoriftworlds.mission_complete",
                "echoriftworlds.save_write",
                "echoriftworlds.save_read",
                "echoriftworlds.packet_send",
                "echoriftworlds.packet_receive",
                "echoriftworlds.region_enter",
                "echoriftworlds.discovery"),
                Set.of(),
                true,
                true,
                false));
    }
}
