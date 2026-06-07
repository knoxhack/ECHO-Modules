package com.knoxhack.echocursecore.integration.lens;

import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CurseCoreLensIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private CurseCoreLensIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            LensProviderRegistry.register(CurseCoreLensProvider.INSTANCE);
        }
    }
}
