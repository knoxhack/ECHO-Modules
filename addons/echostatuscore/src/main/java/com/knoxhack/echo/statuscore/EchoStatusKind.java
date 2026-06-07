package com.knoxhack.echo.statuscore;

import java.util.EnumSet;
import java.util.Set;

public enum EchoStatusKind {
    RADIATION("radiation"),
    TOXICITY("toxicity"),
    HYPOTHERMIA("hypothermia"),
    HEAT_STRESS("heat_stress"),
    INFECTION("infection"),
    MUTATION("mutation"),
    SIGNAL_CORRUPTION("signal_corruption"),
    ARCANE_CURSE("arcane_curse"),
    FATIGUE("fatigue"),
    BLEEDING("bleeding"),
    SHOCK("shock"),
    DEHYDRATION("dehydration"),
    CONTAMINATION("contamination"),
    NEXUS_EXPOSURE("nexus_exposure"),
    BUFF("buff"),
    DEBUFF("debuff"),
    HAZARD("hazard"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    public static final Set<EchoStatusKind> BUILT_IN_STATUS_KINDS = Set.copyOf(EnumSet.of(
            RADIATION,
            TOXICITY,
            HYPOTHERMIA,
            HEAT_STRESS,
            INFECTION,
            MUTATION,
            SIGNAL_CORRUPTION,
            ARCANE_CURSE,
            FATIGUE,
            BLEEDING,
            SHOCK,
            DEHYDRATION,
            CONTAMINATION,
            NEXUS_EXPOSURE
    ));

    private final String serializedName;

    EchoStatusKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
