package com.knoxhack.echoopenlandsprotocol.contract;

import java.util.Map;

public enum OpenlandsAdapterLoadPhase {
    DISCOVER("discover", 10, "Discover module identity and accept the runtime target."),
    LOAD_DATA("load_data", 20, "Load canonical Echo JSON resources before runtime-specific registration."),
    REGISTER_CONTENT("register_content", 30, "Register blocks, items, recipes, tags, loot, and station surfaces."),
    BIND_WORLDGEN("bind_worldgen", 40, "Bind biomes, structures, creatures, ambience, and starter spawn guarantees."),
    BIND_GAMEPLAY_STATE("bind_gameplay_state", 50, "Bind first-hour progression, save data, waystones, HoloMap, and co-op state."),
    READY("ready", 60, "Report runtime readiness after smoke checks and relaxed Standard mode checks."),
    RELEASE_GATE("release_gate", 70, "Prove artifacts, launcher flows, parity tests, and legal review before release.");

    private final String id;
    private final int order;
    private final String description;

    OpenlandsAdapterLoadPhase(String id, int order, String description) {
        this.id = id;
        this.order = order;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public int order() {
        return order;
    }

    public String description() {
        return description;
    }

    public Map<String, Object> asAdapterRecord() {
        return Map.of(
                "id", id,
                "order", order,
                "description", description
        );
    }
}
