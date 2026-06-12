package com.knoxhack.echocore.client.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public final class EchoNamedModelParts {
    private final Map<String, ModelPart> parts;

    private EchoNamedModelParts(Map<String, ModelPart> parts) {
        this.parts = Map.copyOf(parts);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, ModelPart> asMap() {
        return parts;
    }

    public static final class Builder {
        private final Map<String, ModelPart> parts = new LinkedHashMap<>();

        public Builder put(String name, ModelPart part) {
            if (name != null && !name.isBlank() && part != null) {
                parts.put(name, part);
            }
            return this;
        }

        public EchoNamedModelParts build() {
            return new EchoNamedModelParts(parts);
        }
    }
}
