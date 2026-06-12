package com.echoplatform.echocore.api.index;

public enum IndexSourceKind {
    SOURCE_CARD("Source Card"),
    BLOCK_DROP("Block Drop"),
    LOOT_TABLE("Loot Table"),
    WORLDGEN("World Generation"),
    STRUCTURE("Structure"),
    TRADER("Trader"),
    CACHE("Cache"),
    MISSION_REWARD("Mission Reward"),
    ROUTE_UNLOCK("Route Unlock"),
    RESEARCH("Research"),
    MACHINE("Machine"),
    UNKNOWN("Unknown");

    private final String label;

    IndexSourceKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
