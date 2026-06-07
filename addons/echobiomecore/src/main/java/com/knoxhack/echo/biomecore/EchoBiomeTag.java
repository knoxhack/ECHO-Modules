package com.knoxhack.echo.biomecore;

import java.util.Map;

public record EchoBiomeTag(
        String tagId,
        String label,
        Map<String, String> attributes
) {
    public EchoBiomeTag {
        tagId = BiomeContractGuards.normalizedId(tagId, "biome tag id");
        label = BiomeContractGuards.optionalText(label);
        attributes = BiomeContractGuards.immutableMap(attributes);
    }
}
