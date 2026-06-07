package com.knoxhack.echo.cinematiccore;

import java.util.Map;

public record EchoCinematicPacing(
        long fadeInTicks,
        long holdTicks,
        long fadeOutTicks,
        double urgency,
        boolean skippable,
        Map<String, String> attributes
) {
    public EchoCinematicPacing {
        fadeInTicks = CinematicContractGuards.nonNegative(fadeInTicks, "fade in ticks");
        holdTicks = CinematicContractGuards.nonNegative(holdTicks, "hold ticks");
        fadeOutTicks = CinematicContractGuards.nonNegative(fadeOutTicks, "fade out ticks");
        urgency = CinematicContractGuards.clamped01(urgency);
        attributes = CinematicContractGuards.immutableMap(attributes);
    }
}
