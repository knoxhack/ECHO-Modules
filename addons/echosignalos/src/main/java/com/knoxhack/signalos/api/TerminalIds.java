package com.knoxhack.signalos.api;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class TerminalIds {
    private TerminalIds() {
    }

    public static Identifier parse(String value, String label) {
        Identifier id = Identifier.tryParse(value == null ? "" : value.strip());
        return requireLowercase(id, label);
    }

    public static Identifier requireLowercase(Identifier id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(label + " id is required.");
        }
        String value = id.toString();
        if (!value.equals(value.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(label + " id must be lowercase: " + value);
        }
        return id;
    }
}
