package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoassetcore} capabilities in the AdapterCore truth layer.
 */
public final class EchoAssetcoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoassetcore:runtime_host";
    private static final EchoAssetcoreRuntimeHost HOST = new EchoAssetcoreRuntimeHost();

    private EchoAssetcoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events"),
                Set.of(
                "echoassetcore.mission_start",
                "echoassetcore.mission_complete",
                "echoassetcore.ui_open",
                "echoassetcore.ui_close",
                "echoassetcore.button_click",
                "echoassetcore.region_enter",
                "echoassetcore.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
