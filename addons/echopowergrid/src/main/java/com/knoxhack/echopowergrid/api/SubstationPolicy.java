package com.knoxhack.echopowergrid.api;

public enum SubstationPolicy {
    BALANCED,
    LIFE_SUPPORT_FIRST,
    INDUSTRIAL_FIRST,
    NEXUS_STABILIZATION,
    MANUAL;

    public SubstationPolicy next() {
        SubstationPolicy[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
