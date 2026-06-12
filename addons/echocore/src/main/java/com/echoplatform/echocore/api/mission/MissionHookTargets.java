package com.echoplatform.echocore.api.mission;

import java.util.Map;
import net.minecraft.resources.Identifier;

public final class MissionHookTargets {
    private MissionHookTargets() {
    }

    public static Identifier objectiveTarget(String modId, Identifier missionId, String objectiveKey) {
        String missionPath = missionId == null ? "unknown" : missionId.getPath().replace('/', '_');
        String key = objectiveKey == null || objectiveKey.isBlank() ? "objective" : objectiveKey;
        return Identifier.fromNamespaceAndPath(modId, missionPath + "/" + key);
    }

    public static Identifier objectiveTarget(String modId, Identifier missionId, int objectiveIndex) {
        return objectiveTarget(modId, missionId, "objective_" + Math.max(0, objectiveIndex));
    }

    public static Map<String, String> context(String modId, Identifier missionId, String key, String value) {
        return Map.of(
                "module", modId == null ? "" : modId,
                "mission", missionId == null ? "" : missionId.toString(),
                key == null || key.isBlank() ? "detail" : key,
                value == null ? "" : value);
    }
}
