package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;

public final class GalacticCoreBlocks {
    private GalacticCoreBlocks() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeRegistryService registry) {
        for (GalacticCoreContentDefinitions.Registration block : GalacticCoreContentDefinitions.BLOCKS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", block))
            );
        }
    }
}
