package com.knoxhack.echo.progressioncore;

public enum EchoObjectiveType {
    SCAN_BLOCK("scan_block"),
    SCAN_ENTITY("scan_entity"),
    VISIT_REGION("visit_region"),
    CRAFT_ITEM("craft_item"),
    REPAIR_MACHINE("repair_machine"),
    SURVIVE_WEATHER("survive_weather"),
    CLEAR_POI("clear_poi"),
    DECODE_SIGNAL("decode_signal"),
    DEFEAT_GUARDIAN("defeat_guardian"),
    RESTORE_POWER("restore_power"),
    DELIVER_ITEM("deliver_item"),
    ACTIVATE_RELAY("activate_relay"),
    TALK_TO_NPC("talk_to_npc"),
    GAIN_REPUTATION("gain_reputation"),
    DISCOVER_STRUCTURE("discover_structure"),
    UNLOCK_RECIPE("unlock_recipe"),
    RECOVER_BLACKBOX("recover_blackbox"),
    COMPLETE_RITUAL("complete_ritual"),
    STABILIZE_RIFT("stabilize_rift"),
    POWER_GRID_NODE("power_grid_node"),
    ESCORT_CONVOY("escort_convoy"),
    BUILD_BASE_MODULE("build_base_module"),
    CUSTOM("custom"),
    UNKNOWN("unknown");

    private final String serializedName;

    EchoObjectiveType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
