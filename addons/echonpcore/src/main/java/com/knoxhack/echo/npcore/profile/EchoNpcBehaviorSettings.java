package com.knoxhack.echo.npcore.profile;

public record EchoNpcBehaviorSettings(
        String mode,
        int wanderRadius,
        int returnRadius,
        int ambientCooldown,
        boolean stationary,
        boolean homebound) {
    public static final EchoNpcBehaviorSettings DEFAULT =
            new EchoNpcBehaviorSettings("settler_trader", 8, 24, 2400, false, true);

    public EchoNpcBehaviorSettings {
        mode = clean(mode, "settler_trader");
        wanderRadius = Math.max(0, wanderRadius);
        returnRadius = Math.max(wanderRadius, returnRadius);
        ambientCooldown = Math.max(0, ambientCooldown);
    }

    public boolean canWander() {
        return !stationary && wanderRadius > 0;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
