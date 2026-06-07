package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echomachinecore} capabilities in the AdapterCore truth layer.
 */
public final class EchoMachinecoreRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echomachinecore:runtime_host";
    private static final EchoMachinecoreRuntimeHost HOST = new EchoMachinecoreRuntimeHost();

    private EchoMachinecoreRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echomachinecore.machine_use",
                "echomachinecore.machine_state_changed",
                "echomachinecore.region_enter",
                "echomachinecore.discovery"),
                Set.of(),
                true,
                false,
                false));
    }
}
