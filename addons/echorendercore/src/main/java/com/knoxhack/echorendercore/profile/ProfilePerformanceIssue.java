package com.knoxhack.echorendercore.profile;

import net.minecraft.resources.Identifier;

public record ProfilePerformanceIssue(
        Identifier profileId,
        String code,
        ProfileValidationSeverity severity,
        String message,
        int value,
        int threshold
) {
}
