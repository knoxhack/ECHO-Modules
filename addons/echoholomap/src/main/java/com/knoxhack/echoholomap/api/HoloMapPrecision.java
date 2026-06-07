package com.knoxhack.echoholomap.api;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.HoloMapIds;
import net.minecraft.resources.Identifier;

public enum HoloMapPrecision {
    PRECISE,
    ESTIMATED,
    VIRTUAL;

    public static HoloMapPrecision fromCore(IMapMarker marker) {
        if (marker == null) {
            return ESTIMATED;
        }
        if (marker.precise()) {
            return PRECISE;
        }
        Identifier sourceId = marker.sourceId();
        if (HoloMapIds.DISCOVERY_SOURCE.equals(sourceId) || HoloMapIds.ROUTE_SOURCE.equals(sourceId)) {
            return VIRTUAL;
        }
        return ESTIMATED;
    }
}
