package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public record OpenlandsStandardRules(
        String modeId,
        String hunger,
        boolean stamina,
        boolean hydration,
        boolean foodSpoilage,
        boolean temperatureDamage,
        String deathPack,
        String hostileIntensity
) {
    public static OpenlandsStandardRules relaxedDefault() {
        return new OpenlandsStandardRules(
                "openlands_standard",
                "gentle",
                false,
                false,
                false,
                false,
                "recoverable",
                "moderate"
        );
    }

    public boolean hardcoreMetersOff() {
        return !stamina && !hydration && !foodSpoilage && !temperatureDamage;
    }

    public Map<String, Object> asAdapterRecord() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modeId", modeId);
        result.put("hunger", hunger);
        result.put("stamina", stamina);
        result.put("hydration", hydration);
        result.put("foodSpoilage", foodSpoilage);
        result.put("temperatureDamage", temperatureDamage);
        result.put("deathPack", deathPack);
        result.put("hostileIntensity", hostileIntensity);
        result.put("hardcoreMetersOff", hardcoreMetersOff());
        return Map.copyOf(result);
    }
}
