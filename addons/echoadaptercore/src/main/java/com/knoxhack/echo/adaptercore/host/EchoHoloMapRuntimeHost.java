package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echoholomap} capabilities in the AdapterCore truth layer.
 */
public final class EchoHoloMapRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoholomap:runtime_host";
    private static final EchoHoloMapRuntimeHost HOST = new EchoHoloMapRuntimeHost();

    private EchoHoloMapRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Hud", "EchoNativeRuntimeHost.Events"),
                Set.of(
                        "holomap.marker_add",
                        "holomap.marker_remove",
                        "holomap.route_show",
                        "holomap.route_hide",
                        "holomap.zoom_in",
                        "holomap.zoom_out"),
                Set.of(),
                false,
                false,
                true));
    }
}
