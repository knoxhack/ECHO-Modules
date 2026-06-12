package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoFactionAction(Identifier id, String label, String detail, int requiredReputation, boolean serviceAction) {
    public EchoFactionAction {
        label = label == null ? "" : label;
        detail = detail == null ? "" : detail;
    }

    public boolean service() {
        return serviceAction;
    }

    public String description() {
        return detail;
    }
}
