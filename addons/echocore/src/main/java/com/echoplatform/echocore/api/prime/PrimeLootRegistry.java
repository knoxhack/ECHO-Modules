package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeLootRegistry {
    boolean registerPool(PrimeLootPool pool);

    boolean registerInjection(PrimeLootInjection injection);

    List<PrimeLootPool> pools();

    List<PrimeLootInjection> injections();

    record PrimeLootPool(
            Identifier id,
            String title,
            String summary,
            int order) {
        public PrimeLootPool {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
        }
    }

    record PrimeLootInjection(
            Identifier id,
            Identifier poolId,
            Identifier itemId,
            int minCount,
            int maxCount,
            int weight,
            String sourceModule) {
        public PrimeLootInjection {
            minCount = Math.max(0, minCount);
            maxCount = Math.max(minCount, maxCount);
            weight = Math.max(0, weight);
            sourceModule = sourceModule == null ? "" : sourceModule;
        }
    }
}
