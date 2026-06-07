package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoritualcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoRitualcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoritualcore:runtime_host";
    private static final EchoRitualcoreRuntimeHost HOST = new EchoRitualcoreRuntimeHost();

    private EchoRitualcoreRuntimeHost() {
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
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoritualcore.item_crafted",
                "echoritualcore.item_used",
                "echoritualcore.block_placed",
                "echoritualcore.block_broken",
                "echoritualcore.machine_use",
                "echoritualcore.machine_state_changed",
                "echoritualcore.mission_start",
                "echoritualcore.mission_complete",
                "echoritualcore.save_write",
                "echoritualcore.save_read",
                "echoritualcore.ui_open",
                "echoritualcore.ui_close",
                "echoritualcore.button_click",
                "echoritualcore.packet_send",
                "echoritualcore.packet_receive",
                "echoritualcore.region_enter",
                "echoritualcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
