package com.knoxhack.echocore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.EchoServiceRegistry;

/** Foundation entrypoint for the ECHO module graph. */
public final class EchoCore {
    public static final String MOD_ID = "echocore";
    public static final String MODID = MOD_ID;
    public static final String MOD_NAME = "ECHO: Core";
    public static final String VERSION = "1.0.0";

    private EchoCore() {
    }

    public static EchoServiceRegistry services() {
        return EchoCoreServices.registry();
    }

    public static EchoRuntimeModules runtimeModules() {
        return EchoCoreServices.runtimeModules();
    }
}
