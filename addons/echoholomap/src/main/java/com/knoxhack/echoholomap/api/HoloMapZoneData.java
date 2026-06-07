package com.knoxhack.echoholomap.api;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.HoloMapIds;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapZoneData(
        Identifier id,
        Identifier layerId,
        Identifier sourceId,
        HoloMapZoneShape shape,
        HoloMapZonePattern pattern,
        IMapMarker.MarkerState state,
        String title,
        String summary,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float radius,
        float width,
        float depth,
        int fillColor,
        int outlineColor,
        HoloMapPrecision precision,
        int priority,
        List<HoloMapZonePoint> points) {
    public HoloMapZoneData {
        id = id == null ? HoloMapIds.id("zone/unknown") : id;
        layerId = layerId == null ? HoloMapIds.HAZARDS : layerId;
        sourceId = sourceId == null ? id : sourceId;
        shape = shape == null ? HoloMapZoneShape.CIRCLE : shape;
        pattern = pattern == null ? HoloMapZonePattern.SOLID : pattern;
        state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
        title = clean(title, id.getPath());
        summary = summary == null ? "" : summary.strip();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        radius = Math.max(0.0F, radius);
        width = Math.max(0.0F, width);
        depth = Math.max(0.0F, depth);
        fillColor = fillColor == 0 ? 0x335CDAFF : fillColor;
        outlineColor = outlineColor == 0 ? 0xAA5CDAFF : outlineColor;
        precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
        points = List.copyOf(points == null ? List.of() : points.stream().filter(Objects::nonNull).toList());
    }

    public static HoloMapZoneData circle(Identifier id, Identifier layerId, Identifier sourceId,
            HoloMapZonePattern pattern, IMapMarker.MarkerState state, String title, String summary,
            ResourceKey<Level> dimension, double x, double y, double z, float radius,
            int fillColor, int outlineColor, HoloMapPrecision precision, int priority) {
        return new HoloMapZoneData(id, layerId, sourceId, HoloMapZoneShape.CIRCLE, pattern, state,
                title, summary, dimension, x, y, z, radius, radius * 2.0F, radius * 2.0F,
                fillColor, outlineColor, precision, priority, List.of());
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
