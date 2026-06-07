package com.knoxhack.echoterminal.api.recipe;

import net.minecraft.network.chat.Component;

public record TerminalRecipeNote(Component text, boolean warning) {
    public TerminalRecipeNote {
        text = text == null ? Component.empty() : text;
    }

    public static TerminalRecipeNote info(String text) {
        return new TerminalRecipeNote(Component.literal(text == null ? "" : text), false);
    }

    public static TerminalRecipeNote warning(String text) {
        return new TerminalRecipeNote(Component.literal(text == null ? "" : text), true);
    }

    public static TerminalRecipeNote of(Component text) {
        return new TerminalRecipeNote(text, false);
    }

    public static TerminalRecipeNote warning(Component text) {
        return new TerminalRecipeNote(text, true);
    }
}
