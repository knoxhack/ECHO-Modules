package com.echoplatform.echocore.api.index;

public record IndexMachineLayoutGauge(
        String kind,
        String label,
        int x,
        int y,
        int width,
        int height,
        int color) {
    public IndexMachineLayoutGauge {
        kind = kind == null ? "" : kind;
        label = label == null ? "" : label;
        width = Math.max(1, width);
        height = Math.max(1, height);
    }

    public IndexMachineLayoutGauge(String id, int x, int y, int w, int h, String label, float value) {
        this(id, label, x, y, w, h, 0xFF66E8FF);
    }

    public String id() {
        return kind;
    }

    public int w() {
        return width;
    }

    public int h() {
        return height;
    }

    public float value() {
        return 0.65F;
    }
}
