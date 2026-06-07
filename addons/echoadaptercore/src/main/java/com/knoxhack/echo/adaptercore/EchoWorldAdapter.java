package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoWorldAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet worldCapabilities() {
        return EchoCapabilitySet.of(EchoCapabilityId.of("worldgen.features"));
    }
}
