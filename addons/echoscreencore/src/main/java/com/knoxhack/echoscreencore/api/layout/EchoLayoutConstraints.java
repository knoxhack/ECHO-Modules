package com.knoxhack.echoscreencore.api.layout;

public record EchoLayoutConstraints(
    int availableWidth,
    int availableHeight,
    int minWidth,
    int minHeight,
    int maxWidth,
    int maxHeight
) {
    public static EchoLayoutConstraints of(int availableWidth, int availableHeight) {
        return new EchoLayoutConstraints(
            Math.max(0, availableWidth),
            Math.max(0, availableHeight),
            0,
            0,
            Math.max(0, availableWidth),
            Math.max(0, availableHeight)
        );
    }
}
