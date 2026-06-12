package com.echoplatform.echocore.api.mission;

import net.minecraft.resources.Identifier;

public record MissionActionView(String id, String label, boolean enabled, String disabledReason) {
    public MissionActionView {
        id = id == null ? "" : id;
        label = label == null ? "" : label;
        disabledReason = disabledReason == null ? "" : disabledReason;
    }

    public static MissionActionView enabled(Identifier id, String label) {
        return new MissionActionView(id == null ? "" : id.toString(), label, true, "");
    }

    public static MissionActionView enabled(String id, String label) {
        return new MissionActionView(id, label, true, "");
    }

    public static MissionActionView disabled(Identifier id, String label, String reason) {
        return new MissionActionView(id == null ? "" : id.toString(), label, false, reason);
    }

    public static MissionActionView disabled(String id, String label, String reason) {
        return new MissionActionView(id, label, false, reason);
    }
}
