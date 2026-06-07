package com.knoxhack.echogalacticcore.content;

import com.knoxhack.echogalacticcore.asdk.GalacticCoreNativeMutations;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;

import java.util.LinkedHashMap;
import java.util.Map;

final class GalacticCoreRegistrarSupport {
    private GalacticCoreRegistrarSupport() {
    }

    static EchoNativeServiceMutation mutation(
            String surface,
            String action,
            GalacticCoreContentDefinitions.Registration registration
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>(registration.evidence());
        evidence.put("id", registration.id());
        evidence.put("kind", registration.kind());
        evidence.put("legacySource", registration.legacySource());
        evidence.put("source", "galacticraft_legacy_mit_port");
        evidence.put("namespace", "echogalacticcore");
        evidence.put("credit", "Derived from Galacticraft Legacy by TeamGalacticraft under MIT.");
        return GalacticCoreNativeMutations.common(surface, action, registration.id(), evidence);
    }

    static void record(EchoNativeModuleLoadContext context, EchoNativeMutationReceipt receipt) {
        GalacticCoreNativeMutations.record(context, receipt);
    }
}
