package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectLoadRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoStatusEffectLoadResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeStatusEffectLoadBridge {
    private final String moduleId;

    public EchoNativeStatusEffectLoadBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native status effect load module id");
    }

    public EchoStatusEffectLoadResult load(EchoStatusEffectLoadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status effect load request must not be null");
        }
        Map<String, Object> payload = statusPayload(request.savedStatusState(), request.saveKey());
        boolean loaded = !payload.isEmpty();
        return new EchoStatusEffectLoadResult(
                request.playerId(),
                request.hazardId(),
                loaded ? text(payload.get("effectId")) : "",
                loaded ? integer(payload.get("durationTicks")) : 0,
                loaded ? integer(payload.get("amplifier")) : 0,
                request.saveKey(),
                loaded ? floating(payload.get("damageApplied")) : 0.0F,
                loaded ? Math.max(0L, longs(payload.get("gameTick"))) : 0L,
                request.gameTick(),
                request.sourceReason(),
                loaded
        );
    }

    public Map<String, Object> report(EchoStatusEffectLoadRequest request) {
        EchoStatusEffectLoadResult result = load(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_status_effect_load");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("statusEffectLoadResult", result);
        report.put("status", result.loaded() ? "PASS" : "MISSING_SAVE_KEY");
        report.put("summary", "Native Loader backend loaded a persisted hazard status effect through AdapterCore world contracts.");
        return report;
    }

    private static Map<String, Object> statusPayload(Map<String, Object> savedStatusState, String saveKey) {
        Object raw = savedStatusState.get(saveKey);
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                payload.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return payload;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value == null) {
            return 0;
        }
        return Math.max(0, Integer.parseInt(String.valueOf(value)));
    }

    private static long longs(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static float floating(Object value) {
        if (value instanceof Number number) {
            return Math.max(0.0F, number.floatValue());
        }
        if (value == null) {
            return 0.0F;
        }
        return Math.max(0.0F, Float.parseFloat(String.valueOf(value)));
    }
}
