package com.knoxhack.echoholomap.client;

import com.knoxhack.echoholomap.HoloMapIds;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public record HoloMapChunkMenuAction(
        Identifier providerId,
        Identifier actionId,
        String label,
        boolean enabled,
        int color) {
    public HoloMapChunkMenuAction {
        providerId = providerId == null ? HoloMapIds.id("chunk_action/unknown_provider") : providerId;
        actionId = actionId == null ? HoloMapIds.id("chunk_action/unknown_action") : actionId;
        label = label == null || label.isBlank() ? actionId.getPath() : label.strip();
        color = color == 0 ? HoloMapVisualStyle.ACCENT : color;
    }

    public Identifier menuId() {
        return HoloMapIds.id("chunk_action/" + safe(providerId) + "/" + safe(actionId));
    }

    private static String safe(Identifier id) {
        String value = id == null ? "unknown" : id.toString().toLowerCase(Locale.ROOT);
        return value.replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
    }
}
