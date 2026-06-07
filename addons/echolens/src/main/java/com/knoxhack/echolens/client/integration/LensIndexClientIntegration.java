package com.knoxhack.echolens.client.integration;

import com.knoxhack.echolens.EchoLens;

public final class LensIndexClientIntegration {
    private static boolean registered;

    private LensIndexClientIntegration() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        EchoLens.LOGGER.info("ECHO: Lens linked Index client shortcuts.");
        registered = true;
    }
}
