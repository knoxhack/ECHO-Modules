package com.knoxhack.echocore.client.model;

import net.minecraft.resources.Identifier;

public final class EchoMobRenderIds {
    private EchoMobRenderIds() {
    }

    public static Identifier baseTexture(String modId, String entityName) {
        return Identifier.fromNamespaceAndPath(modId, "textures/entity/" + sanitize(entityName) + ".png");
    }

    public static Identifier profile(String modId, String entityName) {
        return Identifier.fromNamespaceAndPath(modId, "echo_mobs/" + sanitize(entityName));
    }

    private static String sanitize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().replace('\\', '/');
    }
}
