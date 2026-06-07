package com.knoxhack.echoashfallprotocol.api.drone;

public enum EchoDroneUpgrade {
    SENSOR_LENS("Sensor Lens"),
    SIGNAL_ANTENNA("Signal Antenna"),
    MICRO_CARGO_POD("Micro Cargo Pod"),
    HAZARD_SENSOR("Hazard Sensor"),
    STABILIZED_BATTERY("Stabilized Battery"),
    UTILITY_LIGHT("Utility Light"),
    THREAT_CLASSIFIER("Threat Classifier"),
    MISSION_DECODER("Mission Decoder");

    private final String displayName;

    EchoDroneUpgrade(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static EchoDroneUpgrade parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EchoDroneUpgrade.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
