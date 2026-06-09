package com.knoxhack.echocore;

import com.knoxhack.echocore.api.EchoCoreServices;

public final class EchoCoreNativeModule {
    public String moduleId() {
        return EchoCore.MOD_ID;
    }

    public void bootstrap() {
        EchoCoreServices.bootstrapDefaults();
    }
}
