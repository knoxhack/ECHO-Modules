package com.knoxhack.echoscreencore.api.contract;

public record EchoScreenSafeAreaPolicy(
    int left,
    int top,
    int right,
    int bottom,
    boolean clampFloatingSurfaces,
    boolean reserveHudEdges,
    boolean allowEmergencyOverlap
) {
    public EchoScreenSafeAreaPolicy {
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.max(0, right);
        bottom = Math.max(0, bottom);
    }

    public static EchoScreenSafeAreaPolicy appSurface() {
        return new EchoScreenSafeAreaPolicy(12, 10, 12, 10, true, false, false);
    }

    public static EchoScreenSafeAreaPolicy hudAware() {
        return new EchoScreenSafeAreaPolicy(14, 14, 14, 18, true, true, true);
    }
}
