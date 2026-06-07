package com.knoxhack.echodatacore.content;

import com.knoxhack.echodatacore.EchoDataCore;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class DataCoreReloaders {
    private DataCoreReloaders() {
    }

    public static Map<Identifier, DataCoreJsonReloadListener> serverReloadListeners() {
        return Map.of(Identifier.fromNamespaceAndPath(EchoDataCore.MODID, "data_keys"),
                new DataCoreJsonReloadListener());
    }
}
