package com.echoplatform.echocore.api.mission;

public enum MissionObjectiveType {
    DISCOVER,
    COLLECT,
    CRAFT,
    BUILD,
    DEFEAT,
    TRAVEL,
    CUSTOM,
    OBTAIN_ITEM,
    DELIVER_ITEM,
    CRAFT_ITEM,
    PLACE_BLOCK,
    DISCOVER_STRUCTURE,
    ENTER_REGION,
    SCAN_BLOCK,
    SCAN_ENTITY,
    KILL_ENTITY,
    ESTABLISH_ROUTE,
    UNLOCK_RESEARCH,
    SURVIVE_TIME,
    REPAIR_MACHINE,
    BUILD_MULTIBLOCK,
    DRIVE_VEHICLE,
    COMPLETE_ORBITAL_SCAN;

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static MissionObjectiveType byId(String id) {
        String normalized = id == null ? "" : id.strip().toLowerCase(java.util.Locale.ROOT);
        for (MissionObjectiveType type : values()) {
            if (type.id().equals(normalized) || type.name().toLowerCase(java.util.Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return CUSTOM;
    }
}
