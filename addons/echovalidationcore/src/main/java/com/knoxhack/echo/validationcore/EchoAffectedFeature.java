package com.knoxhack.echo.validationcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Objects;

public record EchoAffectedFeature(
        EchoFeatureId featureId,
        String label,
        EchoValidationCategory category
) {
    public EchoAffectedFeature {
        Objects.requireNonNull(featureId, "featureId");
        label = label == null || label.isBlank() ? featureId.value() : label.trim();
        category = category == null ? EchoValidationCategory.UNKNOWN : category;
    }

    public static EchoAffectedFeature of(EchoFeatureId featureId) {
        return new EchoAffectedFeature(featureId, "", EchoValidationCategory.UNKNOWN);
    }
}
