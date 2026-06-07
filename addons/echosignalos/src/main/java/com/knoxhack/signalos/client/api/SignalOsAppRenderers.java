package com.knoxhack.signalos.client.api;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SignalOsAppRenderers {
    private static final Map<String, SignalOsAppRenderer> RENDERERS = new ConcurrentHashMap<>();

    private SignalOsAppRenderers() {
    }

    public static void register(String appType, SignalOsAppRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("SignalOS app renderer is required.");
        }
        String type = clean(appType);
        if (type.isBlank()) {
            throw new IllegalArgumentException("SignalOS app renderer type is required.");
        }
        RENDERERS.put(type, renderer);
    }

    public static SignalOsAppRenderer renderer(String appType) {
        return RENDERERS.get(clean(appType));
    }

    public static void clearForTests() {
        RENDERERS.clear();
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
