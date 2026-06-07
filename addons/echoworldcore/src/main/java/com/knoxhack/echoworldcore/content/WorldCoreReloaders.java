package com.knoxhack.echoworldcore.content;

import com.knoxhack.echoworldcore.EchoWorldCore;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class WorldCoreReloaders {
    private WorldCoreReloaders() {
    }

    public static Map<Identifier, WorldCoreJsonReloadListener> serverReloadListeners() {
        return Map.of(Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, "world_definitions"),
                new WorldCoreJsonReloadListener());
    }
}
