package com.knoxhack.echoholomap.client;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class HoloMapGlyphRenderer {
    private HoloMapGlyphRenderer() {
    }

    public static void drawMarker(GuiGraphicsExtractor graphics, HoloMapSnapshotPacket.MarkerData marker,
            int x, int y, int color, int size, boolean selected) {
        if (marker == null) {
            drawGeneric(graphics, x, y, color, size);
            if (selected) {
                drawSelection(graphics, x, y, Math.max(8, size + 5), HoloMapVisualStyle.TEXT);
            }
            return;
        }
        if (marker.radius() > 0.0F && marker.kind() != IMapMarker.MarkerKind.ROUTE) {
            int radius = Math.max(size + 3, Math.min(44, (int) Math.round(marker.radius() / 8.0D)));
            graphics.outline(x - radius, y - radius, radius * 2, radius * 2,
                    HoloMapVisualStyle.withAlpha(color, 0x55));
        }
        if (!drawIcon(graphics, HoloMapIconTextures.marker(marker), x, y, markerIconPixels(size))) {
            drawMarkerFallback(graphics, marker.kind(), marker.state(), x, y, color, size);
        }
        if (selected) {
            drawSelection(graphics, x, y, Math.max(8, size + 5), HoloMapVisualStyle.TEXT);
        }
    }

    public static void drawMarkerKind(GuiGraphicsExtractor graphics, IMapMarker.MarkerKind kind,
            IMapMarker.MarkerState state, int x, int y, int color, int size) {
        if (!drawIcon(graphics, HoloMapIconTextures.markerState(kind, state), x, y, markerIconPixels(size))) {
            drawMarkerFallback(graphics, kind, state, x, y, color, size);
        }
    }

    public static void drawWaypoint(GuiGraphicsExtractor graphics, HoloMapWaypoint waypoint,
            int x, int y, int color, int size, boolean selected) {
        if (waypoint == null) {
            drawWaypointScope(graphics, Scope.LOCAL, x, y, color, size);
            if (selected) {
                drawSelection(graphics, x, y, Math.max(8, size + 5), HoloMapVisualStyle.TEXT);
            }
            return;
        }
        if (!waypoint.visible()) {
            color = HoloMapVisualStyle.MUTED;
        }
        if (!drawIcon(graphics, HoloMapIconTextures.waypoint(waypoint), x, y, markerIconPixels(size))) {
            drawWaypointFallback(graphics, waypoint, x, y, color, size);
        }
        if (selected) {
            drawSelection(graphics, x, y, Math.max(8, size + 5), HoloMapVisualStyle.TEXT);
        }
    }

    public static void drawWaypointScope(GuiGraphicsExtractor graphics, Scope scope,
            int x, int y, int color, int size) {
        if (!drawIcon(graphics, HoloMapIconTextures.waypointScope(scope), x, y, markerIconPixels(size))) {
            drawWaypointScopeFallback(graphics, scope, x, y, color, size);
        }
    }

    public static void drawEdgeIndicator(GuiGraphicsExtractor graphics, int x, int y, int color) {
        if (!drawIcon(graphics, HoloMapIconTextures.EDGE_INDICATOR, x, y, 12)) {
            graphics.outline(x - 3, y - 3, 6, 6, HoloMapVisualStyle.withAlpha(color, 0xCC));
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    public static void drawSelection(GuiGraphicsExtractor graphics, int x, int y, int radius, int color) {
        int iconSize = Math.max(18, radius * 2 + 8);
        if (!drawIcon(graphics, HoloMapIconTextures.SELECTED_RING, x, y, iconSize)) {
            graphics.outline(x - radius, y - radius, radius * 2, radius * 2,
                    HoloMapVisualStyle.withAlpha(color, 0xDD));
            graphics.fill(x - radius - 2, y, x - radius + 3, y + 1, color);
            graphics.fill(x + radius - 2, y, x + radius + 3, y + 1, color);
            graphics.fill(x, y - radius - 2, x + 1, y - radius + 3, color);
            graphics.fill(x, y + radius - 2, x + 1, y + radius + 3, color);
        }
    }

    public static void drawPlayer(GuiGraphicsExtractor graphics, int x, int y, float yawDegrees, int size) {
        int iconSize = markerIconPixels(size);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().rotateAbout((float) Math.toRadians(yawDegrees + 180.0F), x, y);
            if (!drawIcon(graphics, HoloMapIconTextures.PLAYER, x, y, iconSize)) {
                drawPlayerFallback(graphics, x, y, Math.max(4, size));
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }
        int stride = Math.max(1, (int) Math.ceil(steps / 96.0D));
        for (int i = 0; i <= steps; i += stride) {
            int x = x0 + dx * i / steps;
            int y = y0 + dy * i / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
        graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
    }

    private static void drawMarkerFallback(GuiGraphicsExtractor graphics, IMapMarker.MarkerKind kind,
            IMapMarker.MarkerState state, int x, int y, int color, int size) {
        if (state == IMapMarker.MarkerState.LOCKED) {
            drawLocked(graphics, x, y, color, size);
        } else if (state == IMapMarker.MarkerState.CHECKED) {
            drawChecked(graphics, x, y, color, size);
        } else {
            switch (kind == null ? IMapMarker.MarkerKind.GENERIC : kind) {
                case CRASH_SITE -> drawCrashSite(graphics, x, y, color, size);
                case ROUTE -> drawRouteNode(graphics, x, y, color, size);
                case HAZARD -> drawHazard(graphics, x, y, color, size);
                case MISSION -> drawMission(graphics, x, y, color, size);
                case BASE_OUTPOST -> drawBase(graphics, x, y, color, size);
                case ORBITAL_SCAN -> drawOrbital(graphics, x, y, color, size);
                case NEXUS_ANOMALY -> drawAnomaly(graphics, x, y, color, size);
                case DRONE_SCAN -> drawDrone(graphics, x, y, color, size);
                case REGION, GENERIC -> drawGeneric(graphics, x, y, color, size);
            }
        }
    }

    private static void drawWaypointFallback(GuiGraphicsExtractor graphics, HoloMapWaypoint waypoint,
            int x, int y, int color, int size) {
        if (waypoint.isDeathpoint()) {
            drawDeathpoint(graphics, x, y, color, size);
        } else {
            drawWaypointScopeFallback(graphics, waypoint.scope(), x, y, color, size);
        }
    }

    private static void drawWaypointScopeFallback(GuiGraphicsExtractor graphics, Scope scope,
            int x, int y, int color, int size) {
        if (scope == Scope.SHARED) {
            graphics.outline(x - size, y - size, size * 2, size * 2, color);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
            graphics.fill(x - size, y, x + size + 1, y + 1, color);
        } else if (scope == Scope.PERSONAL) {
            graphics.fill(x - 1, y - size - 2, x + 2, y + size + 3, color);
            graphics.fill(x - size - 2, y - 1, x + size + 3, y + 2, color);
            graphics.outline(x - size, y - size, size * 2, size * 2, color);
        } else {
            graphics.fill(x, y - size - 2, x + 1, y - 2, color);
            graphics.fill(x, y + 3, x + 1, y + size + 3, color);
            graphics.fill(x - size - 2, y, x - 2, y + 1, color);
            graphics.fill(x + 3, y, x + size + 3, y + 1, color);
            graphics.outline(x - size, y - size, size * 2, size * 2, color);
        }
    }

    private static boolean drawIcon(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int iconSize) {
        if (iconSize <= 0 || !HoloMapIconTextures.available(texture)) {
            return false;
        }
        int half = iconSize / 2;
        graphics.blit(texture, x - half, y - half, x - half + iconSize, y - half + iconSize,
                0.0F, 1.0F, 0.0F, 1.0F);
        return true;
    }

    private static int markerIconPixels(int size) {
        return Math.max(12, Math.min(28, size * 4));
    }

    private static void drawCrashSite(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.fill(x - 1, y - size - 1, x + 2, y + size + 2, color);
        graphics.fill(x - size - 1, y - 1, x + size + 2, y + 2, color);
        graphics.outline(x - size, y - size, size * 2, size * 2, color);
    }

    private static void drawRouteNode(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.fill(x - size - 1, y - 2, x + size + 2, y + 3, color);
        graphics.fill(x - 2, y - size - 1, x + 3, y + size + 2, color);
    }

    private static void drawHazard(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.outline(x - size, y - size, size * 2, size * 2, color);
        graphics.fill(x - 1, y - size - 2, x + 2, y + size + 3, color);
        graphics.fill(x - size - 2, y - 1, x + size + 3, y + 2, color);
    }

    private static void drawMission(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.fill(x - size, y - size, x + size + 1, y - size + 2, color);
        graphics.fill(x - size, y + size - 1, x + size + 1, y + size + 1, color);
        graphics.fill(x - size, y - size, x - size + 2, y + size + 1, color);
        graphics.fill(x + size - 1, y - size, x + size + 1, y + size + 1, color);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
    }

    private static void drawBase(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.outline(x - size - 1, y - size - 1, (size + 1) * 2, (size + 1) * 2, color);
        graphics.fill(x - size + 1, y - size + 1, x + size, y + size, color);
    }

    private static void drawOrbital(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.outline(x - size - 2, y - size - 2, (size + 2) * 2, (size + 2) * 2,
                HoloMapVisualStyle.withAlpha(color, 0xBB));
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        graphics.fill(x - size - 4, y, x - size, y + 1, color);
        graphics.fill(x + size, y, x + size + 5, y + 1, color);
    }

    private static void drawAnomaly(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.fill(x, y - size - 3, x + 1, y - 2, color);
        graphics.fill(x, y + 3, x + 1, y + size + 4, color);
        graphics.fill(x - size - 3, y, x - 2, y + 1, color);
        graphics.fill(x + 3, y, x + size + 4, y + 1, color);
        graphics.outline(x - size, y - size, size * 2, size * 2, color);
    }

    private static void drawDrone(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.outline(x - size, y - size, size * 2, size * 2, color);
        graphics.fill(x - size, y - size, x - size + 3, y - size + 3, color);
        graphics.fill(x + size - 2, y + size - 2, x + size + 1, y + size + 1, color);
    }

    private static void drawGeneric(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.fill(x - size / 2, y - size / 2, x + size / 2 + 1, y + size / 2 + 1, color);
        graphics.outline(x - size, y - size, size * 2, size * 2, color);
    }

    private static void drawDeathpoint(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.outline(x - size - 1, y - size - 1, (size + 1) * 2, (size + 1) * 2,
                HoloMapVisualStyle.withAlpha(color, 0xBB));
        drawLine(graphics, x - size - 1, y - size - 1, x + size + 1, y + size + 1, color);
        drawLine(graphics, x + size + 1, y - size - 1, x - size - 1, y + size + 1, color);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, HoloMapVisualStyle.TEXT);
    }

    private static void drawLocked(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        graphics.outline(x - size, y - size, size * 2, size * 2, HoloMapVisualStyle.withAlpha(color, 0xAA));
        graphics.fill(x - 1, y - 1, x + 2, y + 2, HoloMapVisualStyle.withAlpha(color, 0xCC));
        graphics.fill(x - size + 2, y - size - 2, x + size - 1, y - size, color);
    }

    private static void drawChecked(GuiGraphicsExtractor graphics, int x, int y, int color, int size) {
        drawGeneric(graphics, x, y, color, size);
        graphics.fill(x - size, y, x - 1, y + 2, HoloMapVisualStyle.TEXT);
        graphics.fill(x - 1, y + 1, x + size + 2, y + 3, HoloMapVisualStyle.TEXT);
    }

    private static void drawPlayerFallback(GuiGraphicsExtractor graphics, int x, int y, int size) {
        int outer = Math.max(8, size + 5);
        int inner = Math.max(5, size + 2);
        int accent = HoloMapVisualStyle.ACCENT;
        int glow = HoloMapVisualStyle.withAlpha(accent, 0xCC);
        int text = HoloMapVisualStyle.TEXT;
        drawLine(graphics, x, y - outer, x - outer, y + outer, glow);
        drawLine(graphics, x, y - outer, x + outer, y + outer, glow);
        drawLine(graphics, x - outer, y + outer, x, y + inner / 2, glow);
        drawLine(graphics, x + outer, y + outer, x, y + inner / 2, glow);
        drawLine(graphics, x, y - inner, x - inner, y + inner, text);
        drawLine(graphics, x, y - inner, x + inner, y + inner, text);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, text);
    }
}
