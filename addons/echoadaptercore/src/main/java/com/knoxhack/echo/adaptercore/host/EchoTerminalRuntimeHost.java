package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echoterminal} capabilities in the AdapterCore truth layer.
 */
public final class EchoTerminalRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoterminal:runtime_host";
    private static final EchoTerminalRuntimeHost HOST = new EchoTerminalRuntimeHost();

    private EchoTerminalRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.Packets", "EchoNativeRuntimeHost.Events"),
                Set.of(
                        "terminal.open",
                        "terminal.close",
                        "terminal.button_click",
                        "terminal.page_navigate"),
                Set.of(),
                false,
                false,
                true));
    }
}
