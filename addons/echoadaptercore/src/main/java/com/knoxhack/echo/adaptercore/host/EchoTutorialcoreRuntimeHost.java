package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echotutorialcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoTutorialcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echotutorialcore:runtime_host";
    private static final EchoTutorialcoreRuntimeHost HOST = new EchoTutorialcoreRuntimeHost();

    private EchoTutorialcoreRuntimeHost() {
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
                "echotutorialcore.item_crafted",
                "echotutorialcore.item_used",
                "echotutorialcore.block_placed",
                "echotutorialcore.block_broken",
                "echotutorialcore.machine_use",
                "echotutorialcore.machine_state_changed",
                "echotutorialcore.mission_start",
                "echotutorialcore.mission_complete",
                "echotutorialcore.save_write",
                "echotutorialcore.save_read",
                "echotutorialcore.ui_open",
                "echotutorialcore.ui_close",
                "echotutorialcore.button_click",
                "echotutorialcore.packet_send",
                "echotutorialcore.packet_receive",
                "echotutorialcore.region_enter",
                "echotutorialcore.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
