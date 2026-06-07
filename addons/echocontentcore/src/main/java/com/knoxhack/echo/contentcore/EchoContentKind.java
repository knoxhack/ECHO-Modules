package com.knoxhack.echo.contentcore;

public enum EchoContentKind {
    ITEM("item"),
    BLOCK("block"),
    ENTITY("entity"),
    FLUID("fluid"),
    RECIPE("recipe"),
    MACHINE_RECIPE("machine_recipe"),
    MISSION("mission"),
    OBJECTIVE("objective"),
    REWARD("reward"),
    REGION("region"),
    POI("poi"),
    STRUCTURE("structure"),
    FACTION("faction"),
    NPC("npc"),
    DIALOGUE("dialogue"),
    LOOT_TABLE("loot_table"),
    SOUND_PROFILE("sound_profile"),
    RENDER_PROFILE("render_profile"),
    PARTICLE_PROFILE("particle_profile"),
    WEATHER_EVENT("weather_event"),
    STATUS_EFFECT("status_effect"),
    HOLOMAP_LAYER("holomap_layer"),
    LENS_SCAN("lens_scan"),
    TERMINAL_TAB("terminal_tab"),
    GUIDE_PAGE("guide_page"),
    CODEX_ENTRY("codex_entry"),
    LORE_ENTRY("lore_entry"),
    TEXTURE("texture"),
    MODEL("model"),
    ICON("icon"),
    ANIMATION("animation"),
    VEHICLE("vehicle"),
    MACHINE("machine"),
    POWER_NODE("power_node"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoContentKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean assetLike() {
        return this == TEXTURE || this == MODEL || this == ICON || this == ANIMATION
                || this == SOUND_PROFILE || this == RENDER_PROFILE || this == PARTICLE_PROFILE;
    }

    public boolean gameplayLike() {
        return this == MISSION || this == OBJECTIVE || this == REWARD || this == FACTION
                || this == NPC || this == DIALOGUE || this == STATUS_EFFECT || this == WEATHER_EVENT;
    }
}
