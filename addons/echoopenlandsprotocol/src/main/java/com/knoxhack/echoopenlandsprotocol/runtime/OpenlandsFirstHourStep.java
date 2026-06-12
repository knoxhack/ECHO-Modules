package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.Arrays;
import java.util.List;

public enum OpenlandsFirstHourStep {
    SAFE_SPAWN("safe_spawn", 1, 0, 2),
    FIRST_GATHERING("first_gathering", 2, 2, 8),
    FIRST_TOOLS("first_tools", 3, 8, 15),
    FIRST_SHELTER("first_shelter", 4, 15, 30),
    SLEEP_AND_RECOVER("sleep_and_recover", 5, 30, 35),
    FIRST_EXPLORATION_HOOK("first_exploration_hook", 6, 35, 45),
    FIRST_WAYSTONE("first_waystone", 7, 45, 60);

    private final String id;
    private final int order;
    private final int targetMinuteStart;
    private final int targetMinuteEnd;

    OpenlandsFirstHourStep(String id, int order, int targetMinuteStart, int targetMinuteEnd) {
        this.id = id;
        this.order = order;
        this.targetMinuteStart = targetMinuteStart;
        this.targetMinuteEnd = targetMinuteEnd;
    }

    public String id() {
        return id;
    }

    public int order() {
        return order;
    }

    public int targetMinuteStart() {
        return targetMinuteStart;
    }

    public int targetMinuteEnd() {
        return targetMinuteEnd;
    }

    public static List<String> stepIds() {
        return Arrays.stream(values()).map(OpenlandsFirstHourStep::id).toList();
    }
}
