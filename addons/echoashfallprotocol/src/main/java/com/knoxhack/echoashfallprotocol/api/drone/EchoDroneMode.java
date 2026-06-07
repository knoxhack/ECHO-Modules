package com.knoxhack.echoashfallprotocol.api.drone;

public enum EchoDroneMode {
    FOLLOW("Follow", "Following Operator"),
    ASSIST("Assist", "Field assistance active"),
    SCOUT("Scout", "Scouting ahead"),
    SALVAGE("Salvage", "Recovering salvage"),
    GUARD("Guard", "Watching for threats"),
    DOCK("Dock", "Docked or charging"),
    RECALL("Recall", "Returning to Operator");

    private final String displayName;
    private final String taskLabel;

    EchoDroneMode(String displayName, String taskLabel) {
        this.displayName = displayName;
        this.taskLabel = taskLabel;
    }

    public String displayName() {
        return displayName;
    }

    public String taskLabel() {
        return taskLabel;
    }

    public static EchoDroneMode parse(String value, EchoDroneMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return EchoDroneMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
