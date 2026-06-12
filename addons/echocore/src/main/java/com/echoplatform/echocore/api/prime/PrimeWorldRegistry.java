package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeWorldRegistry {
    boolean registerStructure(PrimeStructure structure);

    boolean registerWorldSignal(PrimeWorldSignal signal);

    List<PrimeStructure> structures();

    List<PrimeWorldSignal> worldSignals();

    record PrimeStructure(
            Identifier id,
            String title,
            String summary,
            Identifier markerType,
            Identifier lootPool,
            int order) {
        public PrimeStructure {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
        }
    }

    record PrimeWorldSignal(
            Identifier id,
            String title,
            String summary,
            int signalLevel,
            int order) {
        public PrimeWorldSignal {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
            signalLevel = Math.max(0, signalLevel);
        }
    }
}
