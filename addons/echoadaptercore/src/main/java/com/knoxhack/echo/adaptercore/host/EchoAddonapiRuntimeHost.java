package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoaddonapi} capabilities in the AdapterCore truth layer.
 */
public final class EchoAddonapiRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoaddonapi:runtime_host";
    private static final EchoAddonapiRuntimeHost HOST = new EchoAddonapiRuntimeHost();

    private EchoAddonapiRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echoaddonapi.item_crafted",
                "echoaddonapi.item_used",
                "echoaddonapi.block_placed",
                "echoaddonapi.block_broken",
                "echoaddonapi.machine_use",
                "echoaddonapi.machine_state_changed",
                "echoaddonapi.mission_start",
                "echoaddonapi.mission_complete"),
                Set.of(),
                false,
                false,
                false));
    }
}
