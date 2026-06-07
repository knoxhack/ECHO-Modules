package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoResourceAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet resourceCapabilities() {
        return EchoCapabilitySet.of(
                EchoCapabilityId.of("resources.assets"),
                EchoCapabilityId.of("resources.data")
        );
    }
}
