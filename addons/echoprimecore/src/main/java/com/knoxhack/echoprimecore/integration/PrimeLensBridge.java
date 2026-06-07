package com.knoxhack.echoprimecore.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;

public final class PrimeLensBridge {
    private PrimeLensBridge() {
    }

    public static void register(PrimeIntegrationRegistry registry) {
        for (PrimeLensRegistry.PrimeScanType type : registry.scanTypes()) {
            EchoCoreServices.lensService().registerScanType(type.id(), type.title());
        }
    }
}
