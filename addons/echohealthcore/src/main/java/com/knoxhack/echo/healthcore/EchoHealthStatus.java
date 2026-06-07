package com.knoxhack.echo.healthcore;

import java.util.Collection;

public enum EchoHealthStatus {
    HEALTHY("healthy", 0),
    WARNING("warning", 10),
    DEGRADED("degraded", 20),
    CRITICAL("critical", 30),
    DISABLED("disabled", 20),
    UNKNOWN("unknown", 10);

    private final String serializedName;
    private final int rank;

    EchoHealthStatus(String serializedName, int rank) {
        this.serializedName = serializedName;
        this.rank = rank;
    }

    public String serializedName() {
        return serializedName;
    }

    public int rank() {
        return rank;
    }

    public boolean requiresAttention() {
        return rank >= WARNING.rank;
    }

    public static EchoHealthStatus worseOf(EchoHealthStatus first, EchoHealthStatus second) {
        EchoHealthStatus safeFirst = first == null ? UNKNOWN : first;
        EchoHealthStatus safeSecond = second == null ? UNKNOWN : second;
        return safeFirst.rank >= safeSecond.rank ? safeFirst : safeSecond;
    }

    public static EchoHealthStatus worst(Collection<EchoHealthStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return UNKNOWN;
        }
        EchoHealthStatus worst = HEALTHY;
        for (EchoHealthStatus status : statuses) {
            worst = worseOf(worst, status);
        }
        return worst;
    }
}
