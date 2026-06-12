package com.echoplatform.echocore.api.prime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EchoPrimeIntegrations {
    private static final CopyOnWriteArrayList<EchoPrimeIntegration> INTEGRATIONS = new CopyOnWriteArrayList<>();

    private EchoPrimeIntegrations() {
    }

    public static boolean register(EchoPrimeIntegration integration) {
        if (integration == null || INTEGRATIONS.stream().anyMatch(value -> value.id().equals(integration.id()))) {
            return false;
        }
        return INTEGRATIONS.add(integration);
    }

    public static List<EchoPrimeIntegration> integrations() {
        return List.copyOf(INTEGRATIONS);
    }

    public static int applyTo(PrimeIntegrationContext context) {
        int applied = 0;
        for (EchoPrimeIntegration integration : INTEGRATIONS) {
            if (integration.available(context)) {
                integration.registerPrime(context);
                applied++;
            }
        }
        return applied;
    }
}
