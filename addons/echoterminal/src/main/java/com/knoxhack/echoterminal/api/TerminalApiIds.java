package com.knoxhack.echoterminal.api;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class TerminalApiIds {
    private TerminalApiIds() {
    }

    public static Identifier requireLowercase(Identifier id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(label + " id is required.");
        }
        if (!id.getNamespace().equals(id.getNamespace().toLowerCase(Locale.ROOT))
                || !id.getPath().equals(id.getPath().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(label + " id must be lowercase: " + id);
        }
        return id;
    }
}
