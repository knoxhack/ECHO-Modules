package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echorecovery} capabilities in the AdapterCore truth layer.
 */
public final class EchoRecoveryRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echorecovery:runtime_host";
    private static final EchoRecoveryRuntimeHost HOST = new EchoRecoveryRuntimeHost();

    private EchoRecoveryRuntimeHost() {
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
                "echorecovery.item_crafted",
                "echorecovery.item_used",
                "echorecovery.block_placed",
                "echorecovery.block_broken",
                "echorecovery.machine_use",
                "echorecovery.machine_state_changed",
                "echorecovery.mission_start",
                "echorecovery.mission_complete",
                "echorecovery.save_write",
                "echorecovery.save_read",
                "echorecovery.ui_open",
                "echorecovery.ui_close",
                "echorecovery.button_click",
                "echorecovery.packet_send",
                "echorecovery.packet_receive",
                "echorecovery.region_enter",
                "echorecovery.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
