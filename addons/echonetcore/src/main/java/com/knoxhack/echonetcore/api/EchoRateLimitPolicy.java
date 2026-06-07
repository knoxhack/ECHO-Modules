package com.knoxhack.echonetcore.api;

public record EchoRateLimitPolicy(int ticks, String scope) {
    public static final EchoRateLimitPolicy NONE = new EchoRateLimitPolicy(0, "");

    public EchoRateLimitPolicy {
        ticks = Math.max(0, ticks);
        scope = scope == null ? "" : scope.trim();
    }

    public static EchoRateLimitPolicy of(int ticks, String scope) {
        return new EchoRateLimitPolicy(ticks, scope);
    }

    public boolean enabled() {
        return ticks > 0;
    }
}
