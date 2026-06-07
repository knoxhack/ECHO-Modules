package com.knoxhack.echoscreencore.api.theme;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record EchoThemeTokenSnapshot(
    Identifier themeId,
    Map<String, Integer> colors,
    Map<String, Integer> spacing,
    Map<String, Integer> font,
    Map<String, Integer> radius
) {
}
