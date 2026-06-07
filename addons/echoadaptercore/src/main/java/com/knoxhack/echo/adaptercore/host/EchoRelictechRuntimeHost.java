package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echorelictech} capabilities in the AdapterCore truth layer.
 */
public final class EchoRelictechRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echorelictech:runtime_host";
    private static final EchoRelictechRuntimeHost HOST = new EchoRelictechRuntimeHost();

    private EchoRelictechRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echorelictech.item_crafted",
                "echorelictech.item_used",
                "echorelictech.block_placed",
                "echorelictech.block_broken",
                "echorelictech.machine_use",
                "echorelictech.machine_state_changed",
                "echorelictech.mission_start",
                "echorelictech.mission_complete",
                "echorelictech.save_write",
                "echorelictech.save_read",
                "echorelictech.ui_open",
                "echorelictech.ui_close",
                "echorelictech.button_click",
                "echorelictech.packet_send",
                "echorelictech.packet_receive",
                "echorelictech.region_enter",
                "echorelictech.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
