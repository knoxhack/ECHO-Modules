package com.knoxhack.echotextureforge.api.scan;

import com.knoxhack.echotextureforge.api.spec.TextureResolution;

public record TextureValidationRules(
        TextureResolution defaultResolution,
        boolean validate32x32,
        boolean requirePowerOfTwo,
        boolean requireTransparentItems,
        boolean strictMode) {
    public static TextureValidationRules defaults() {
        return new TextureValidationRules(TextureResolution.DEFAULT_32, true, true, true, false);
    }
}
