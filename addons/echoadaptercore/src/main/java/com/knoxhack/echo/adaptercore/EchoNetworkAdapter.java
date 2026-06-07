package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoNetworkAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet networkCapabilities() {
        return EchoCapabilitySet.of(EchoCapabilityId.of("network.custom_payload"));
    }
}
