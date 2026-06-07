package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCapabilityId;
import com.knoxhack.echo.platformcore.EchoCapabilitySet;

public interface EchoScreenAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoCapabilitySet screenCapabilities() {
        return EchoCapabilitySet.of(EchoCapabilityId.of("screen.custom"));
    }
}
