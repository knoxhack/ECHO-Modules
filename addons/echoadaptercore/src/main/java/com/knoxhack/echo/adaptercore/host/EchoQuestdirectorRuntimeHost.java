package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoquestdirector} capabilities in the AdapterCore truth layer.
 */
public final class EchoQuestdirectorRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoquestdirector:runtime_host";
    private static final EchoQuestdirectorRuntimeHost HOST = new EchoQuestdirectorRuntimeHost();

    private EchoQuestdirectorRuntimeHost() {
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
                "echoquestdirector.mission_start",
                "echoquestdirector.mission_complete",
                "echoquestdirector.ui_open",
                "echoquestdirector.ui_close",
                "echoquestdirector.button_click",
                "echoquestdirector.region_enter",
                "echoquestdirector.discovery"),
                Set.of(),
                true,
                false,
                true));
    }
}
