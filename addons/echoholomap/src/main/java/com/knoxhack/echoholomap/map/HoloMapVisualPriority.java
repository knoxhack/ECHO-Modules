package com.knoxhack.echoholomap.map;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.Config;

public final class HoloMapVisualPriority {
    private HoloMapVisualPriority() {
    }

    public static int statePriority(IMapMarker.MarkerState state) {
        return switch (state == null ? IMapMarker.MarkerState.DISCOVERED : state) {
            case CHECKED -> 0;
            case DISCOVERED -> 1;
            case LOCKED -> 2;
            case HIDDEN -> 3;
        };
    }

    public static int kindPriority(IMapMarker.MarkerKind kind) {
        return switch (kind == null ? IMapMarker.MarkerKind.GENERIC : kind) {
            case MISSION -> 0;
            case HAZARD -> 1;
            case ROUTE -> 2;
            case BASE_OUTPOST -> 3;
            case CRASH_SITE -> 4;
            case ORBITAL_SCAN -> 5;
            case NEXUS_ANOMALY -> 6;
            case DRONE_SCAN -> 7;
            case REGION -> 8;
            case GENERIC -> 9;
        };
    }

    public static double drawPriority(double distance, IMapMarker.MarkerState state, IMapMarker.MarkerKind kind,
            boolean selected) {
        double selectedWeight = selected ? -10_000.0D : 0.0D;
        return selectedWeight + Math.max(0.0D, distance)
                + statePriority(state) * 250.0D
                + kindPriority(kind) * 12.0D;
    }

    public static boolean shouldDrawLabel(Config.LabelMode mode, boolean selected, boolean nearby,
            int drawnLabels, int labelLimit) {
        if (labelLimit <= 0 || mode == Config.LabelMode.OFF) {
            return false;
        }
        if (selected) {
            return mode == Config.LabelMode.SELECTED || mode == Config.LabelMode.NEARBY;
        }
        return mode == Config.LabelMode.NEARBY && nearby && drawnLabels < labelLimit;
    }
}
