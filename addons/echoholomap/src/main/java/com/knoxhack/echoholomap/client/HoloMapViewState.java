package com.knoxhack.echoholomap.client;

import com.knoxhack.echoholomap.map.HoloMapVisibility;

public final class HoloMapViewState {
    private final String dimension;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final double centerX;
    private final double centerZ;
    private final double zoom;
    private final boolean showMarkers;
    private final HoloMapVisibility.FieldMode fieldMode;
    private final boolean showWaypoints;
    private final String selectedMarkerId;
    private final String selectedWaypointId;
    private final int mouseX;
    private final int mouseY;
    private final double playerX;
    private final double playerZ;
    private final float playerYaw;

    public HoloMapViewState(String dimension, int x, int y, int width, int height,
            double centerX, double centerZ, double zoom, boolean showMarkers, boolean showWaypoints,
            String selectedMarkerId, String selectedWaypointId, int mouseX, int mouseY,
            double playerX, double playerZ, float playerYaw) {
        this(dimension, x, y, width, height, centerX, centerZ, zoom, showMarkers,
                HoloMapVisibility.FieldMode.AUTO_NEAR, showWaypoints, selectedMarkerId, selectedWaypointId,
                mouseX, mouseY, playerX, playerZ, playerYaw);
    }

    public HoloMapViewState(String dimension, int x, int y, int width, int height,
            double centerX, double centerZ, double zoom, boolean showMarkers,
            HoloMapVisibility.FieldMode fieldMode, boolean showWaypoints,
            String selectedMarkerId, String selectedWaypointId, int mouseX, int mouseY,
            double playerX, double playerZ, float playerYaw) {
        this.dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.strip();
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.zoom = Math.max(0.1D, zoom);
        this.showMarkers = showMarkers;
        this.fieldMode = fieldMode == null ? HoloMapVisibility.FieldMode.AUTO_NEAR : fieldMode;
        this.showWaypoints = showWaypoints;
        this.selectedMarkerId = selectedMarkerId == null ? "" : selectedMarkerId;
        this.selectedWaypointId = selectedWaypointId == null ? "" : selectedWaypointId;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.playerX = playerX;
        this.playerZ = playerZ;
        this.playerYaw = playerYaw;
    }

    public String dimension() {
        return dimension;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public double zoom() {
        return zoom;
    }

    public boolean showMarkers() {
        return showMarkers;
    }

    public HoloMapVisibility.FieldMode fieldMode() {
        return fieldMode;
    }

    public boolean showWaypoints() {
        return showWaypoints;
    }

    public String selectedMarkerId() {
        return selectedMarkerId;
    }

    public String selectedWaypointId() {
        return selectedWaypointId;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public double playerX() {
        return playerX;
    }

    public double playerZ() {
        return playerZ;
    }

    public float playerYaw() {
        return playerYaw;
    }

    public int worldToScreenX(double worldX) {
        return x + width / 2 + (int) Math.round((worldX - centerX) * zoom);
    }

    public int worldToScreenZ(double worldZ) {
        return y + height / 2 + (int) Math.round((worldZ - centerZ) * zoom);
    }

    public double screenToWorldX(double screenX) {
        return centerX + (screenX - (x + width / 2.0D)) / zoom;
    }

    public double screenToWorldZ(double screenY) {
        return centerZ + (screenY - (y + height / 2.0D)) / zoom;
    }

    public int minChunkX() {
        return Math.floorDiv((int) Math.floor(screenToWorldX(x + 4)), 16) - 1;
    }

    public int maxChunkX() {
        return Math.floorDiv((int) Math.floor(screenToWorldX(x + width - 4)), 16) + 1;
    }

    public int minChunkZ() {
        return Math.floorDiv((int) Math.floor(screenToWorldZ(y + 4)), 16) - 1;
    }

    public int maxChunkZ() {
        return Math.floorDiv((int) Math.floor(screenToWorldZ(y + height - 4)), 16) + 1;
    }

    public boolean inViewport(int px, int py, int padding) {
        return px >= x - padding && px <= x + width + padding
                && py >= y - padding && py <= y + height + padding;
    }

    public int viewportBucket() {
        int centerBucketX = (int) Math.floor(centerX / 4.0D);
        int centerBucketZ = (int) Math.floor(centerZ / 4.0D);
        return (((x * 31 + y) * 31 + width) * 31 + height) * 31
                + centerBucketX * 31 + centerBucketZ;
    }

    public int zoomBucket() {
        return (int) Math.round(zoom * 100.0D);
    }
}
