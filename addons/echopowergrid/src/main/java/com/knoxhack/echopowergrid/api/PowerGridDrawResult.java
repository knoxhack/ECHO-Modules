package com.knoxhack.echopowergrid.api;

public record PowerGridDrawResult(
        long requested,
        long drawn,
        boolean simulated,
        EchoGridState state) {
    public PowerGridDrawResult {
        requested = Math.max(0L, requested);
        drawn = Math.max(0L, Math.min(drawn, requested));
        state = state == null ? EchoGridState.OFFLINE : state;
    }

    public boolean satisfied() {
        return drawn >= requested;
    }

    public long missing() {
        return Math.max(0L, requested - drawn);
    }

    public static PowerGridDrawResult empty(long requested, boolean simulated) {
        return new PowerGridDrawResult(requested, 0L, simulated, EchoGridState.OFFLINE);
    }
}
