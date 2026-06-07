package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.registry.LensProviderRegistry;

public final class LensBuiltins {
    private static boolean registered;

    private LensBuiltins() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        LensProviderRegistry.registerAll(java.util.List.of(
                TargetIdentityProvider.INSTANCE,
                BlockStatsProvider.INSTANCE,
                FluidStateProvider.INSTANCE,
                EntityStatsProvider.INSTANCE,
                MachineStatusProvider.INSTANCE,
                SafeInventoryProvider.INSTANCE,
                IntegrationStatusProvider.INSTANCE,
                WorldContextProvider.INSTANCE,
                ServerBlockEntityProvider.INSTANCE,
                ServerPrivacyProvider.INSTANCE,
                ServerProgressionProvider.INSTANCE,
                BeginnerHintProvider.INSTANCE));
        registered = true;
    }

    public static synchronized void resetForTests() {
        registered = false;
        LensProviderRegistry.clearForTests();
    }
}
