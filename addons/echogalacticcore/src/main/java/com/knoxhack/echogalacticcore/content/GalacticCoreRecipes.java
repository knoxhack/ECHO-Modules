package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;

public final class GalacticCoreRecipes {
    private GalacticCoreRecipes() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeRegistryService registry) {
        for (GalacticCoreContentDefinitions.Registration recipe : GalacticCoreContentDefinitions.RECIPES) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.registerRecipe(GalacticCoreRegistrarSupport.mutation("registry", "registerRecipe", recipe))
            );
        }
    }
}
