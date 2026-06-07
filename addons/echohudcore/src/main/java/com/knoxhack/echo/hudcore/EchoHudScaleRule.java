package com.knoxhack.echo.hudcore;

import java.util.Map;

public record EchoHudScaleRule(
        String ruleId,
        double minimumScale,
        double defaultScale,
        double maximumScale,
        boolean userConfigurable,
        Map<String, String> attributes
) {
    public EchoHudScaleRule {
        ruleId = HudContractGuards.id(ruleId, "hud scale rule id");
        minimumScale = HudContractGuards.positive(minimumScale, "minimum hud scale");
        defaultScale = HudContractGuards.positive(defaultScale, "default hud scale");
        maximumScale = HudContractGuards.positive(maximumScale, "maximum hud scale");
        attributes = HudContractGuards.immutableMap(attributes);
    }

    public boolean ordered() {
        return minimumScale <= defaultScale && defaultScale <= maximumScale;
    }
}
