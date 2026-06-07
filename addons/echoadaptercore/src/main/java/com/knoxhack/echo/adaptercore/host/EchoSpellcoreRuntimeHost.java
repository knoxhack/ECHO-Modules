package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echospellcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoSpellcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echospellcore:runtime_host";
    private static final EchoSpellcoreRuntimeHost HOST = new EchoSpellcoreRuntimeHost();

    private EchoSpellcoreRuntimeHost() {
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
                "echospellcore.item_crafted",
                "echospellcore.item_used",
                "echospellcore.block_placed",
                "echospellcore.block_broken",
                "echospellcore.machine_use",
                "echospellcore.machine_state_changed",
                "echospellcore.mission_start",
                "echospellcore.mission_complete",
                "echospellcore.save_write",
                "echospellcore.save_read",
                "echospellcore.ui_open",
                "echospellcore.ui_close",
                "echospellcore.button_click",
                "echospellcore.packet_send",
                "echospellcore.packet_receive",
                "echospellcore.region_enter",
                "echospellcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
