package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeProgressionRegistry {
    boolean registerFlag(PrimeProgressionFlag flag);

    List<PrimeProgressionFlag> flags();

    record PrimeProgressionFlag(
            Identifier id,
            String title,
            String summary,
            boolean starter,
            int order) {
        public PrimeProgressionFlag {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
        }
    }
}
