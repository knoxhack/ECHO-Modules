package com.echoplatform.echocore.api;

import java.util.Optional;

public final class EchoOptionalServices {
    private EchoOptionalServices() {
    }

    public static IRuntimeBudgetService runtimeGuardOrNoOp() {
        return EchoCoreServices.runtimeBudgetService();
    }

    public static ISoundService soundCoreOrNoOp() {
        return EchoCoreServices.soundService();
    }

    public static Optional<ISoundService> soundCore() {
        return EchoServiceRegistry.find(ISoundService.class);
    }

    public static IThemeService themeCoreOrNoOp() {
        return EchoCoreServices.themeService();
    }
}
