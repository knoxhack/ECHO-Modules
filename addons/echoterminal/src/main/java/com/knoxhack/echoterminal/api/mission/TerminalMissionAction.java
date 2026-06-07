package com.knoxhack.echoterminal.api.mission;

public record TerminalMissionAction(String id, String label, boolean enabled, String disabledReason) {
    public TerminalMissionAction {
        id = id == null ? "" : id;
        label = label == null || label.isBlank() ? id : label;
        disabledReason = disabledReason == null ? "" : disabledReason;
    }

    public static TerminalMissionAction enabled(String id, String label) {
        return new TerminalMissionAction(id, label, true, "");
    }

    public static TerminalMissionAction disabled(String id, String label, String reason) {
        return new TerminalMissionAction(id, label, false, reason);
    }
}
