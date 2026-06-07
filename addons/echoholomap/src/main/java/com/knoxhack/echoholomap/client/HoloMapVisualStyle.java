package com.knoxhack.echoholomap.client;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import net.minecraft.world.entity.player.Player;

public final class HoloMapVisualStyle {
    public static final int ACCENT = 0xFF38DFF4;
    public static final int TEXT = 0xFFD9F7FF;
    public static final int MUTED = 0xFF9FB4BE;
    public static final int SUCCESS = 0xFF92F7A6;
    public static final int WARNING = 0xFFFFDA73;
    public static final int DANGER = 0xFFFF6688;

    private HoloMapVisualStyle() {
    }

    public static int markerColor(HoloMapSnapshotPacket.MarkerData marker) {
        return markerColor(null, marker);
    }

    public static int markerColor(Player player, HoloMapSnapshotPacket.MarkerData marker) {
        if (marker.state() == IMapMarker.MarkerState.LOCKED) {
            return muted(player);
        }
        if (marker.state() == IMapMarker.MarkerState.CHECKED) {
            return success(player);
        }
        return switch (marker.kind()) {
            case CRASH_SITE -> 0xFFFFA05B;
            case ROUTE -> success(player);
            case HAZARD -> danger(player);
            case MISSION -> accent(player);
            case BASE_OUTPOST -> warning(player);
            case ORBITAL_SCAN -> 0xFFA58BFF;
            case NEXUS_ANOMALY -> HoloMapThemeCoreStyle.color(player, "accent", 0xFFFF8FEA);
            case DRONE_SCAN -> 0xFF7CF7D4;
            case REGION, GENERIC -> text(player);
        };
    }

    public static int accent(Player player) {
        return HoloMapThemeCoreStyle.color(player, "primary", ACCENT);
    }

    public static int text(Player player) {
        return HoloMapThemeCoreStyle.color(player, "text", TEXT);
    }

    public static int muted(Player player) {
        return HoloMapThemeCoreStyle.color(player, "mutedText", MUTED);
    }

    public static int success(Player player) {
        return HoloMapThemeCoreStyle.color(player, "success", SUCCESS);
    }

    public static int warning(Player player) {
        return HoloMapThemeCoreStyle.color(player, "warning", WARNING);
    }

    public static int danger(Player player) {
        return HoloMapThemeCoreStyle.color(player, "error", DANGER);
    }

    public static int panel(Player player) {
        return HoloMapThemeCoreStyle.color(player, "panel", 0xB8061014);
    }

    public static float hologramOpacity(Player player) {
        return HoloMapThemeCoreStyle.intensity(player, "hologramOpacity", 0.72F);
    }

    public static int terrainColor(int color) {
        return adjustColor(color, terrainBrightness(), terrainContrast());
    }

    public static int adjustColor(int color, double brightness, double contrast) {
        int alpha = color >>> 24;
        int red = adjustChannel((color >>> 16) & 0xFF, brightness, contrast);
        int green = adjustChannel((color >>> 8) & 0xFF, brightness, contrast);
        int blue = adjustChannel(color & 0xFF, brightness, contrast);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int adjustChannel(int value, double brightness, double contrast) {
        double normalized = value / 255.0D;
        double contrasted = ((normalized - 0.5D) * contrast + 0.5D) * brightness;
        return clamp((int) Math.round(contrasted * 255.0D), 0, 255);
    }

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static int markerScalePixels(int base) {
        return Math.max(3, (int) Math.round(base * markerScale()));
    }

    public static double markerScale() {
        try {
            return clamp(Config.MINIMAP_MARKER_SCALE.get(), 0.7D, 1.6D);
        } catch (RuntimeException exception) {
            return 1.0D;
        }
    }

    public static double terrainBrightness() {
        try {
            return clamp(Config.MINIMAP_TERRAIN_BRIGHTNESS.get(), 0.55D, 1.45D);
        } catch (RuntimeException exception) {
            return 1.0D;
        }
    }

    public static double terrainContrast() {
        try {
            return clamp(Config.MINIMAP_TERRAIN_CONTRAST.get(), 0.55D, 1.65D);
        } catch (RuntimeException exception) {
            return 1.08D;
        }
    }

    public static Config.LabelMode labelMode() {
        try {
            return Config.MINIMAP_LABEL_MODE.get();
        } catch (RuntimeException exception) {
            return Config.LabelMode.SELECTED;
        }
    }

    public static Config.VisualDensity visualDensity() {
        try {
            return Config.MAP_VISUAL_DENSITY.get();
        } catch (RuntimeException exception) {
            return Config.VisualDensity.MEDIUM;
        }
    }

    public static boolean booleanConfig(com.knoxhack.echocore.api.config.EchoNativeConfigSpec.BooleanValue value,
            boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
