package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;
import dev.echo.nativeplatform.contracts.EchoNativeWorldgenService;

public final class GalacticCoreDimensions {
    private GalacticCoreDimensions() {
    }

    public static void register(
            EchoNativeModuleLoadContext context,
            EchoNativeRegistryService registry,
            EchoNativeWorldgenService worldgen
    ) {
        for (GalacticCoreContentDefinitions.Registration dimension : GalacticCoreContentDefinitions.DIMENSIONS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", dimension))
            );
            GalacticCoreRegistrarSupport.record(
                    context,
                    worldgen.registerFeature(GalacticCoreRegistrarSupport.mutation("worldgen", "registerFeature", dimension))
            );
        }
        for (GalacticCoreContentDefinitions.Registration route : GalacticCoreContentDefinitions.CELESTIAL_ROUTES) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", route))
            );
        }
    }
}
