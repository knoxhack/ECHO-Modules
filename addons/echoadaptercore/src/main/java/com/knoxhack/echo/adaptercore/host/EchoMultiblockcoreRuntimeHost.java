package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echomultiblockcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoMultiblockcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echomultiblockcore:runtime_host";
    private static final EchoMultiblockcoreRuntimeHost HOST = new EchoMultiblockcoreRuntimeHost();

    private EchoMultiblockcoreRuntimeHost() {
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
                "echomultiblockcore.item_crafted",
                "echomultiblockcore.item_used",
                "echomultiblockcore.block_placed",
                "echomultiblockcore.block_broken",
                "echomultiblockcore.machine_use",
                "echomultiblockcore.machine_state_changed",
                "echomultiblockcore.mission_start",
                "echomultiblockcore.mission_complete",
                "echomultiblockcore.save_write",
                "echomultiblockcore.save_read",
                "echomultiblockcore.ui_open",
                "echomultiblockcore.ui_close",
                "echomultiblockcore.button_click",
                "echomultiblockcore.packet_send",
                "echomultiblockcore.packet_receive",
                "echomultiblockcore.region_enter",
                "echomultiblockcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
