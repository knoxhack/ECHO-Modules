package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeRouteRegistry {
    boolean registerRoute(PrimeRoute route);

    List<PrimeRoute> routes();

    record PrimeRoute(
            Identifier id,
            String title,
            String summary,
            Identifier unlockFlag,
            List<String> requiredModules,
            int order,
            int color) {
        public PrimeRoute {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
            requiredModules = requiredModules == null ? List.of() : List.copyOf(requiredModules);
        }
    }
}
