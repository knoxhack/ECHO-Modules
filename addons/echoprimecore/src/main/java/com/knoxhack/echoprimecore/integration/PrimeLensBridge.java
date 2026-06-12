package com.knoxhack.echoprimecore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;

public final class PrimeLensBridge {
    private PrimeLensBridge() {
    }

    public static void register(PrimeIntegrationRegistry registry) {
        for (PrimeLensRegistry.PrimeScanType type : registry.scanTypes()) {
            EchoCoreServices.lensService().registerScanType(type.id(), type.title());
        }
    }
}
