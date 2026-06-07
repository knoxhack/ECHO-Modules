package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoCommandAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet commandCapabilities() {
        return EchoCapabilitySet.of(EchoCapabilityId.of("commands.literal"));
    }
}
