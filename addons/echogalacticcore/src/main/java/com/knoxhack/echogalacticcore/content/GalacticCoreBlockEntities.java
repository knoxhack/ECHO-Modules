package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;

public final class GalacticCoreBlockEntities {
    private GalacticCoreBlockEntities() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeRegistryService registry) {
        for (GalacticCoreContentDefinitions.Registration blockEntity : GalacticCoreContentDefinitions.BLOCK_ENTITIES) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.registerBlockEntity(GalacticCoreRegistrarSupport.mutation(
                            "registry",
                            "registerBlockEntity",
                            blockEntity
                    ))
            );
        }
    }
}
