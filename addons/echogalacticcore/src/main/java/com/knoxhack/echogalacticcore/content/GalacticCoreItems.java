package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;

public final class GalacticCoreItems {
    private GalacticCoreItems() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeRegistryService registry) {
        for (GalacticCoreContentDefinitions.Registration item : GalacticCoreContentDefinitions.ITEMS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", item))
            );
        }
        GalacticCoreRegistrarSupport.record(
                context,
                registry.registerCreativeTab(GalacticCoreRegistrarSupport.mutation(
                        "registry",
                        "registerCreativeTab",
                        new GalacticCoreContentDefinitions.Registration(
                                "creative_tab",
                                "creative_tab",
                                "micdoodle8.mods.galacticraft.core.GalacticraftCore#galacticraftItemsTab",
                                java.util.Map.of("title", "ECHO: GalacticCore")
                        )
                ))
        );
    }
}
