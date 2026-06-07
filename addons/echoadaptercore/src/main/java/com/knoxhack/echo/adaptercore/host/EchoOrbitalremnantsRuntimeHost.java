package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoorbitalremnants} capabilities in the AdapterCore truth layer.
 */
public final class EchoOrbitalremnantsRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoorbitalremnants:runtime_host";
    private static final EchoOrbitalremnantsRuntimeHost HOST = new EchoOrbitalremnantsRuntimeHost();

    private EchoOrbitalremnantsRuntimeHost() {
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
                "echoorbitalremnants.item_crafted",
                "echoorbitalremnants.item_used",
                "echoorbitalremnants.block_placed",
                "echoorbitalremnants.block_broken",
                "echoorbitalremnants.machine_use",
                "echoorbitalremnants.machine_state_changed",
                "echoorbitalremnants.mission_start",
                "echoorbitalremnants.mission_complete",
                "echoorbitalremnants.save_write",
                "echoorbitalremnants.save_read",
                "echoorbitalremnants.ui_open",
                "echoorbitalremnants.ui_close",
                "echoorbitalremnants.button_click",
                "echoorbitalremnants.packet_send",
                "echoorbitalremnants.packet_receive",
                "echoorbitalremnants.region_enter",
                "echoorbitalremnants.discovery"),
                Set.of(),
                true,
                true,
                true));
    }
}
