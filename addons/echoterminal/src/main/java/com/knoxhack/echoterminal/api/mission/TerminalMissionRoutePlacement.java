package com.knoxhack.echoterminal.api.mission;

/**
 * Optional aggregate-route metadata for mission providers.
 * <p>
 * The owning chapter still controls mission state and actions; this only tells
 * the shared Survival Route where and how to summarize the record.
 */
public record TerminalMissionRoutePlacement(
        int phaseOrder,
        int missionOrder,
        TerminalMissionRole role,
        boolean includeInSurvivalRoute) {
    public TerminalMissionRoutePlacement {
        phaseOrder = Math.max(0, Math.min(15, phaseOrder));
        missionOrder = Math.max(0, missionOrder);
        role = role == null ? TerminalMissionRole.MAIN : role;
    }

    public static TerminalMissionRoutePlacement main(int phaseOrder, int missionOrder) {
        return new TerminalMissionRoutePlacement(phaseOrder, missionOrder, TerminalMissionRole.MAIN, true);
    }

    public static TerminalMissionRoutePlacement optional(int phaseOrder, int missionOrder) {
        return new TerminalMissionRoutePlacement(phaseOrder, missionOrder, TerminalMissionRole.OPTIONAL, true);
    }

    public static TerminalMissionRoutePlacement reference(int phaseOrder, int missionOrder) {
        return new TerminalMissionRoutePlacement(phaseOrder, missionOrder, TerminalMissionRole.REFERENCE, true);
    }

    public static TerminalMissionRoutePlacement hidden() {
        return new TerminalMissionRoutePlacement(9, 0, TerminalMissionRole.REFERENCE, false);
    }
}
