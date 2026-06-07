package com.knoxhack.echoscreencore.api.action;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoActionRegistry {
    private static final Map<String, EchoAction> ACTIONS = new LinkedHashMap<>();

    private EchoActionRegistry() {
    }

    public static void register(Identifier id, EchoAction action) {
        if (id != null) {
            register(id.toString(), action);
        }
    }

    public static void register(String id, EchoAction action) {
        if (id == null || id.isBlank() || action == null) {
            return;
        }
        ACTIONS.put(id.trim(), action);
    }

    public static Optional<EchoAction> action(String id) {
        return Optional.ofNullable(id == null ? null : ACTIONS.get(id));
    }

    public static Map<String, EchoAction> actions() {
        return Collections.unmodifiableMap(ACTIONS);
    }

    public static void clearForTests() {
        ACTIONS.clear();
    }
}
