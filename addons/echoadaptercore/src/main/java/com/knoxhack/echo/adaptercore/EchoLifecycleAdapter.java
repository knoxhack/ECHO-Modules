package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoLifecycleAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet lifecycleCapabilities() {
        return EchoCapabilitySet.of(
                EchoCapabilityId.of("lifecycle.common_setup"),
                EchoCapabilityId.of("lifecycle.client_setup"),
                EchoCapabilityId.of("lifecycle.server_setup")
        );
    }
}
