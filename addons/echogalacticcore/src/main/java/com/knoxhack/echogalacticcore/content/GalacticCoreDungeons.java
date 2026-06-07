package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;
import dev.echo.nativeplatform.contracts.EchoNativeResourceService;
import dev.echo.nativeplatform.contracts.EchoNativeWorldgenService;

public final class GalacticCoreDungeons {
    private GalacticCoreDungeons() {
    }

    public static void register(
            EchoNativeModuleLoadContext context,
            EchoNativeRegistryService registry,
            EchoNativeWorldgenService worldgen,
            EchoNativeResourceService resources
    ) {
        for (GalacticCoreContentDefinitions.Registration dungeon : GalacticCoreContentDefinitions.DUNGEONS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", dungeon))
            );
            GalacticCoreRegistrarSupport.record(
                    context,
                    worldgen.placeStructure(GalacticCoreRegistrarSupport.mutation("worldgen", "placeStructure", dungeon))
            );
        }
        for (GalacticCoreContentDefinitions.Registration boss : GalacticCoreContentDefinitions.BOSSES) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.deferredRegister(GalacticCoreRegistrarSupport.mutation("registry", "deferredRegister", boss))
            );
        }
        for (GalacticCoreContentDefinitions.Registration loot : GalacticCoreContentDefinitions.DUNGEON_LOOT) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    registry.registerLootModifier(GalacticCoreRegistrarSupport.mutation("registry", "registerLootModifier", loot))
            );
            GalacticCoreRegistrarSupport.record(
                    context,
                    resources.registerReloadListener(GalacticCoreRegistrarSupport.mutation("resources", "registerReloadListener", loot))
            );
        }
    }
}
