package com.knoxhack.echoscreencore.client.layout;

public record EchoResponsiveContext(
    int viewportWidth,
    int viewportHeight,
    double guiScale,
    EchoBreakpoint activeBreakpoint
) {
    public static EchoResponsiveContext of(int viewportWidth, int viewportHeight, double guiScale) {
        return new EchoResponsiveContext(
            Math.max(1, viewportWidth),
            Math.max(1, viewportHeight),
            Math.max(1.0D, guiScale),
            EchoBreakpoint.active(viewportWidth)
        );
    }
}
