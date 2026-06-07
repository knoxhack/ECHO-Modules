package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoRuntimeSide;

public interface EchoServerAdapter {
    EchoAdapterId adapterId();

    EchoAdapterStatus status();

    default EchoRuntimeSide side() {
        return EchoRuntimeSide.SERVER;
    }
}
