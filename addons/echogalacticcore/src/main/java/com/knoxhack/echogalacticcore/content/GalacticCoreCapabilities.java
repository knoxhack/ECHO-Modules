package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeCapabilityService;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

public final class GalacticCoreCapabilities {
    private GalacticCoreCapabilities() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeCapabilityService capabilities) {
        for (GalacticCoreContentDefinitions.Registration capability : GalacticCoreContentDefinitions.CAPABILITIES) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    capabilities.register(GalacticCoreRegistrarSupport.mutation("capabilities", "register", capability))
            );
        }
    }
}
