package com.knoxhack.echo.adaptercore;

import java.util.List;

public interface EchoRegistryRuntimeBackend {
    String runtimeId();

    EchoRegistryRuntimeResolution resolve(
            EchoRegistryContractSnapshot snapshot,
            List<String> terminalPages,
            List<String> indexEntries
    );
}
