package com.knoxhack.echoashfallprotocol;

import java.util.List;
import java.util.Map;

public final class AshfallNativeAgent9TechModuleEntrypointsVerifier {
    private AshfallNativeAgent9TechModuleEntrypointsVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> result = AshfallNativeAgent9TechModuleEntrypoints.execute(Map.of("packId", "ashfall"));
        require(result, "status", "PASS");
        require(result, "adapterCoreContract", AshfallNativeAgent9TechRuntime.CONTRACT_ID);
        require(result, "runtime", "echo_native_loader");
        require(result, "moduleEntrypointCount", 13);

        Object entries = result.get("moduleEntrypoints");
        if (!(entries instanceof List<?> moduleEntrypoints) || moduleEntrypoints.size() != 13) {
            throw new IllegalStateException("Expected thirteen Agent 9 module entrypoints but found " + entries + ".");
        }
        for (Object item : moduleEntrypoints) {
            if (!(item instanceof Map<?, ?> entry)) {
                throw new IllegalStateException("Module entrypoint is not a map: " + item + ".");
            }
            require(entry, "status", "PASS");
            Object behaviors = entry.get("executedBehaviorIds");
            if (!(behaviors instanceof List<?> behaviorIds) || behaviorIds.isEmpty()) {
                throw new IllegalStateException("Module entrypoint has no executed behaviors: " + entry + ".");
            }
            Object evidence = entry.get("behaviorEvidence");
            if (!(evidence instanceof List<?> behaviorEvidence) || behaviorEvidence.size() != behaviorIds.size()) {
                throw new IllegalStateException("Module entrypoint behavior evidence does not match behaviors: " + entry + ".");
            }
        }

        System.out.println("Ashfall native Agent 9 tech module entrypoints verifier PASS");
    }

    private static void require(Map<?, ?> data, String key, Object expected) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }
}
