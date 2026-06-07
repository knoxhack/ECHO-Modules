package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;

public interface EchoFeaturePolicy {
    boolean exclusive(EchoFeatureId featureId);

    boolean trustBlocked(String trustLevel, boolean officialPack);
}
