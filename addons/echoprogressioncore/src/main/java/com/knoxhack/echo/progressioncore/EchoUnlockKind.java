package com.knoxhack.echo.progressioncore;

public enum EchoUnlockKind {
    ROUTE_GATE("route_gate"),
    CHAPTER_GATE("chapter_gate"),
    FEATURE_UNLOCK("feature_unlock"),
    RECIPE_UNLOCK("recipe_unlock"),
    TERMINAL_TAB_UNLOCK("terminal_tab_unlock"),
    LENS_SCAN_UNLOCK("lens_scan_unlock"),
    HOLOMAP_LAYER_UNLOCK("holomap_layer_unlock"),
    WORLD_EVENT_UNLOCK("world_event_unlock"),
    FACTION_UNLOCK("faction_unlock"),
    ARCANA_UNLOCK("arcana_unlock"),
    TECH_TREE_GATE("tech_tree_gate"),
    CONTENT_GATE("content_gate"),
    OBJECTIVE_GATE("objective_gate"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoUnlockKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean uiSurfaceUnlock() {
        return this == TERMINAL_TAB_UNLOCK || this == LENS_SCAN_UNLOCK || this == HOLOMAP_LAYER_UNLOCK;
    }
}
