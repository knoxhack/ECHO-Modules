package com.knoxhack.echoholomap.api;

import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.HoloMapIds;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapRouteData(
        Identifier id,
        Identifier layerId,
        Identifier sourceId,
        String title,
        String summary,
        ResourceKey<Level> dimension,
        int color,
        IMapMarker.MarkerState state,
        List<HoloMapRoutePoint> points) {
    public HoloMapRouteData {
        id = id == null ? HoloMapIds.id("route/unknown") : id;
        layerId = layerId == null ? HoloMapIds.ROUTES : layerId;
        sourceId = sourceId == null ? id : sourceId;
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        summary = summary == null ? "" : summary.strip();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        color = color == 0 ? 0xFF92F7A6 : color;
        state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
        points = points == null ? List.of() : points.stream()
                .filter(point -> point != null)
                .sorted(Comparator.comparingInt(HoloMapRoutePoint::order))
                .toList();
    }
}
