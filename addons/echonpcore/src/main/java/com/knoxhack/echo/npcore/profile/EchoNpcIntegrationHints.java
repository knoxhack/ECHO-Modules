package com.knoxhack.echo.npcore.profile;

public record EchoNpcIntegrationHints(
        boolean terminalContact,
        boolean mapMarker,
        boolean discoverOnInteract,
        String intelSummary) {
    public static final EchoNpcIntegrationHints DEFAULT =
            new EchoNpcIntegrationHints(true, true, true, "");

    public EchoNpcIntegrationHints {
        intelSummary = intelSummary == null ? "" : intelSummary.trim();
    }
}
