package com.knoxhack.echoholomap.api;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.HoloMapIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapOverlayData(
        Identifier id,
        Identifier layerId,
        Identifier sourceId,
        HoloMapOverlayKind kind,
        IMapMarker.MarkerState state,
        String title,
        String summary,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float radius,
        int color,
        HoloMapPrecision precision) {
    public HoloMapOverlayData {
        id = id == null ? HoloMapIds.id("overlay/unknown") : id;
        layerId = layerId == null ? HoloMapIds.HAZARDS : layerId;
        sourceId = sourceId == null ? id : sourceId;
        kind = kind == null ? HoloMapOverlayKind.CIRCLE : kind;
        state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        summary = summary == null ? "" : summary.strip();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        radius = Math.max(0.0F, radius);
        color = color == 0 ? 0x66FF5C7A : color;
        precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
    }
}
