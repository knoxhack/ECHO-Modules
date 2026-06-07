package com.knoxhack.echoashfallprotocol;

import java.util.List;
import java.util.Map;

public final class AshfallNativeMachinePowerResourceAuditVerifier {
    private AshfallNativeMachinePowerResourceAuditVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> result = AshfallNativeMachinePowerResourceAudit.run(Map.of("packId", "ashfall"));
        require(result, "status", "PASS");
        require(result, "adapterCoreBridge", true);
        require(result, "standaloneDuplicateGameplaySystem", false);
        require(result, "nativeReportVisibility", true);
        require(result, "minecraftRuntimeAccessed", false);
        require(result, "auditedBlockCount", 17);
        require(result, "passingBlockCount", 17L);

        Object blocks = result.get("auditedBlocks");
        if (!(blocks instanceof List<?> auditedBlocks)) {
            throw new IllegalStateException("Expected auditedBlocks list but found " + blocks);
        }
        for (Object block : auditedBlocks) {
            if (!(block instanceof Map<?, ?> entry)) {
                throw new IllegalStateException("Expected audited block map but found " + block);
            }
            require(entry, "complete", true);
            require(entry, "nativeReportVisible", true);
        }

        System.out.println("Ashfall native machine/power resource audit verifier PASS");
    }

    private static void require(Map<?, ?> data, String key, Object expected) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }
}
