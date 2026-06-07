package com.knoxhack.echospellcore.integration.lens;

import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpellCoreLensIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private SpellCoreLensIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            LensProviderRegistry.register(SpellCoreLensProvider.INSTANCE);
        }
    }
}
