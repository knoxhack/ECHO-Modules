package com.knoxhack.echoterminal.client.mission;

public enum TerminalMissionNoticeType {
    MISSION_AVAILABLE("MISSION AVAILABLE"),
    OBJECTIVE_READY("OBJECTIVE READY"),
    CACHE_READY("CACHE READY"),
    CACHE_CLAIMED("CACHE CLAIMED"),
    PHASE_ONLINE("PHASE ONLINE"),
    SUMMARY("MISSION SYNC");

    private final String label;

    TerminalMissionNoticeType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
