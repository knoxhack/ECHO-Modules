package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;

public final class GalacticCoreEntities {
    private GalacticCoreEntities() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeRegistryService registry) {
        for (GalacticCoreContentDefinitions.Registration entity : GalacticCoreContentDefinitions.ENTITIES) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", entity))
            );
        }
    }
}
