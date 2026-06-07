package com.knoxhack.echothemecore.api;

import net.minecraft.resources.Identifier;

public record ThemeTransition(
    Identifier fromTheme,
    Identifier toTheme,
    TransitionStyle style,
    int durationTicks,
    int primaryColor,
    int secondaryColor,
    float glowStrength,
    float particleStrength
) {
}
