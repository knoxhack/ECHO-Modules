package com.knoxhack.echoterminal.api;

import net.minecraft.resources.Identifier;

public record TerminalTabDescriptor(Identifier id, String title, int order, int accentColor) {
    public TerminalTabDescriptor {
        TerminalApiIds.requireLowercase(id, "Terminal tab");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Terminal tab title is required.");
        }
    }
}
