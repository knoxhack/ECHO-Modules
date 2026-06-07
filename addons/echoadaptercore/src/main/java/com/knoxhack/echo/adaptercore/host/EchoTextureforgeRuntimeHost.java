package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echotextureforge} capabilities in the AdapterCore truth layer.
 */
public final class EchoTextureforgeRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echotextureforge:runtime_host";
    private static final EchoTextureforgeRuntimeHost HOST = new EchoTextureforgeRuntimeHost();

    private EchoTextureforgeRuntimeHost() {
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
                "echotextureforge.block_placed",
                "echotextureforge.block_broken",
                "echotextureforge.machine_use",
                "echotextureforge.machine_state_changed",
                "echotextureforge.mission_start",
                "echotextureforge.mission_complete",
                "echotextureforge.ui_open",
                "echotextureforge.ui_close",
                "echotextureforge.button_click",
                "echotextureforge.packet_send",
                "echotextureforge.packet_receive",
                "echotextureforge.region_enter",
                "echotextureforge.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
