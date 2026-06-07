package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echowiki} capabilities in the AdapterCore truth layer.
 */
public final class EchoWikiRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echowiki:runtime_host";
    private static final EchoWikiRuntimeHost HOST = new EchoWikiRuntimeHost();

    private EchoWikiRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echowiki.item_crafted",
                "echowiki.item_used",
                "echowiki.machine_use",
                "echowiki.machine_state_changed",
                "echowiki.mission_start",
                "echowiki.mission_complete",
                "echowiki.ui_open",
                "echowiki.ui_close",
                "echowiki.button_click",
                "echowiki.packet_send",
                "echowiki.packet_receive",
                "echowiki.region_enter",
                "echowiki.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
