package com.knoxhack.echoholomap.map;

import com.knoxhack.echocore.api.IMapMarker;

public final class HoloMapVisibility {
    public static final double AUTO_FIELD_RADIUS_BLOCKS = 192.0D;
    public static final int AUTO_FIELD_FILL_BUDGET = 6;
    public static final int AUTO_FIELD_OUTLINE_BUDGET = 12;

    private HoloMapVisibility() {
    }

    public static boolean visibleInNormalView(IMapMarker.MarkerState state) {
        return state == IMapMarker.MarkerState.DISCOVERED || state == IMapMarker.MarkerState.CHECKED;
    }

    public static boolean markerCanGenerateField(IMapMarker.MarkerKind kind) {
        return switch (kind == null ? IMapMarker.MarkerKind.GENERIC : kind) {
            case HAZARD, REGION, CRASH_SITE, ORBITAL_SCAN, NEXUS_ANOMALY, DRONE_SCAN -> true;
            case MISSION, ROUTE, BASE_OUTPOST, GENERIC -> false;
        };
    }

    public enum FieldMode {
        AUTO_NEAR("AUTO"),
        ALL("ALL"),
        OFF("OFF");

        private final String label;

        FieldMode(String label) {
            this.label = label;
        }

        public FieldMode next() {
            FieldMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public String label() {
            return label;
        }
    }
}
