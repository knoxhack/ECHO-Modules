package com.knoxhack.echothemecore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoThemeBlockPalette(
    List<Identifier> recommendedBlocks,
    List<Identifier> panelBlocks,
    List<Identifier> glassBlocks,
    List<Identifier> lightBlocks,
    List<Identifier> accentBlocks
) {
    public EchoThemeBlockPalette {
        recommendedBlocks = recommendedBlocks == null ? List.of() : List.copyOf(recommendedBlocks);
        panelBlocks = panelBlocks == null ? List.of() : List.copyOf(panelBlocks);
        glassBlocks = glassBlocks == null ? List.of() : List.copyOf(glassBlocks);
        lightBlocks = lightBlocks == null ? List.of() : List.copyOf(lightBlocks);
        accentBlocks = accentBlocks == null ? List.of() : List.copyOf(accentBlocks);
    }
}
