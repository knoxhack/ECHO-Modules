package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeTerminalRegistry {
    boolean registerCard(PrimeTerminalCard card);

    List<PrimeTerminalCard> cards();

    record PrimeTerminalCard(
            Identifier id,
            Identifier routeId,
            String title,
            String summary,
            Identifier unlockFlag,
            String sourceModule,
            int order) {
    }
}
