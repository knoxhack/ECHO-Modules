package com.echoplatform.echocore.api;

public record EchoHazardTelemetry(
        int hydration,
        int radiation,
        int toxicAir,
        int oxygen,
        int pressure,
        int cold,
        int heat,
        int exposure,
        String statusLine) {
    public EchoHazardTelemetry {
        statusLine = statusLine == null ? "" : statusLine;
    }

    public static EchoHazardTelemetry nominal() {
        return new EchoHazardTelemetry(100, 0, 0, 100, 100, 0, 0, 0, "Nominal.");
    }

    public boolean warning() {
        return hydration < 35 || radiation > 25 || toxicAir > 25 || oxygen < 70 || pressure < 70
                || cold > 30 || heat > 30 || exposure > 30;
    }

    public boolean safeZone() {
        return !warning();
    }

    public String summary() {
        return statusLine;
    }

    public int severity() {
        int severity = 0;
        if (hydration < 35) {
            severity++;
        }
        if (radiation > 25) {
            severity++;
        }
        if (toxicAir > 25) {
            severity++;
        }
        if (oxygen < 70 || pressure < 70) {
            severity++;
        }
        if (cold > 30 || heat > 30 || exposure > 30) {
            severity++;
        }
        return severity;
    }
}
