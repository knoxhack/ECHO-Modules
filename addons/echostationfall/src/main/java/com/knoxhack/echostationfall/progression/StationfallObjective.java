package com.knoxhack.echostationfall.progression;

import java.util.Locale;

public enum StationfallObjective {
    HYDROPONICS_PURGE(
            "hydroponics_purge",
            StationSection.HYDROPONICS_BAY,
            "Purge Growth Clusters",
            "Clear corrupted growth from Hydroponics.",
            3
    ),
    MEDICAL_MANIFEST(
            "medical_manifest",
            StationSection.MEDICAL_WING,
            "Recover Medical Manifest",
            "Decode the Medical Wing manifest.",
            1
    ),
    ENGINEERING_BREAKER(
            "engineering_breaker",
            StationSection.ENGINEERING_DECK,
            "Restart Relay Breaker",
            "Restore Engineering relay power.",
            1
    ),
    CONTAINMENT_RECORDS(
            "containment_records",
            StationSection.CONTAINMENT_WING,
            "Unlock Pod Records",
            "Query containment pod records.",
            3
    ),
    OBSERVATION_ANTENNA(
            "observation_antenna",
            StationSection.OBSERVATION_DECK,
            "Align External Antenna",
            "Align the Observation Deck antenna feed.",
            2
    );

    private final String key;
    private final StationSection section;
    private final String title;
    private final String hint;
    private final int targetSteps;

    StationfallObjective(String key, StationSection section, String title, String hint, int targetSteps) {
        this.key = key;
        this.section = section;
        this.title = title;
        this.hint = hint;
        this.targetSteps = Math.max(1, targetSteps);
    }

    public String key() {
        return key;
    }

    public StationSection section() {
        return section;
    }

    public String title() {
        return title;
    }

    public String hint() {
        return hint;
    }

    public int targetSteps() {
        return targetSteps;
    }

    public static StationfallObjective byKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        for (StationfallObjective objective : values()) {
            if (objective.key.equals(normalized) || objective.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return objective;
            }
        }
        return HYDROPONICS_PURGE;
    }

    public static StationfallObjective bySection(StationSection section) {
        for (StationfallObjective objective : values()) {
            if (objective.section == section) {
                return objective;
            }
        }
        return null;
    }
}
