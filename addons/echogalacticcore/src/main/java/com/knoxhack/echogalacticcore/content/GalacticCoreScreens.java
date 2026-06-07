package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeScreenService;

public final class GalacticCoreScreens {
    private GalacticCoreScreens() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeScreenService screens) {
        for (GalacticCoreContentDefinitions.Registration screen : GalacticCoreContentDefinitions.SCREENS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    screens.registerSurface(GalacticCoreRegistrarSupport.mutation("screens", "registerSurface", screen))
            );
            GalacticCoreRegistrarSupport.record(
                    context,
                    screens.registerMenu(GalacticCoreRegistrarSupport.mutation(
                            "screens",
                            "registerMenu",
                            new GalacticCoreContentDefinitions.Registration(
                                    screen.path() + "_menu",
                                    "screen_menu",
                                    screen.legacySource(),
                                    screen.evidence()
                            )
                    ))
            );
        }
    }
}
