package com.knoxhack.echoarcanacore.api;

import com.knoxhack.echoarcanacore.EchoArcanaCore;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public enum AetherSignalType {
    RAW_AETHER("raw_aether"),
    REFINED_AETHER("refined_aether"),
    CURSED_AETHER("cursed_aether"),
    RIFT_AETHER("rift_aether"),
    SOUL_AETHER("soul_aether"),
    SIGNAL_AETHER("signal_aether"),
    VEIL_RESONANCE("veil_resonance"),
    FRACTURE_ENERGY("fracture_energy");

    private final String id;

    AetherSignalType(String id) {
        this.id = id;
    }

    public String serializedName() {
        return id;
    }

    public Identifier identifier() {
        return EchoArcanaCore.id("aether/" + id);
    }

    public static AetherSignalType byId(String value) {
        if (value == null || value.isBlank()) {
            return RAW_AETHER;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AetherSignalType type : values()) {
            if (type.id.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return RAW_AETHER;
    }
}
