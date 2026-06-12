package com.echoplatform.echocore.api.prime;

import net.minecraft.resources.Identifier;

public interface EchoPrimeIntegration {
    Identifier id();

    default boolean available(PrimeIntegrationContext context) {
        return true;
    }

    void registerPrime(PrimeIntegrationContext context);
}
