package com.knoxhack.echoritualcore.integration.lens;

import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RitualCoreLensIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private RitualCoreLensIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            LensProviderRegistry.register(RitualCoreLensProvider.INSTANCE);
        }
    }
}
