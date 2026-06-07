package com.knoxhack.echoscreencore.api.layout;

public record EchoMeasureResult(int width, int height) {
    public static final EchoMeasureResult ZERO = new EchoMeasureResult(0, 0);

    public EchoMeasureResult clamp(EchoLayoutConstraints constraints) {
        if (constraints == null) {
            return this;
        }
        return new EchoMeasureResult(
            clamp(width, constraints.minWidth(), constraints.maxWidth()),
            clamp(height, constraints.minHeight(), constraints.maxHeight())
        );
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
