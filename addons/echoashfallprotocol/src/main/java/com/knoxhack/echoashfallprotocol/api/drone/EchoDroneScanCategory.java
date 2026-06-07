package com.knoxhack.echoashfallprotocol.api.drone;

public enum EchoDroneScanCategory {
    MISSION(1, "objective hint"),
    HAZARD(2, "hazard"),
    HOSTILE(3, "hostile"),
    LOOT(4, "scrap cache"),
    RESOURCE(5, "resource"),
    CONTAINER(6, "container");

    private final int priority;
    private final String summaryName;

    EchoDroneScanCategory(int priority, String summaryName) {
        this.priority = priority;
        this.summaryName = summaryName;
    }

    public int priority() {
        return priority;
    }

    public String summaryName() {
        return summaryName;
    }
}
