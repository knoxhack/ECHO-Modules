package com.knoxhack.echolens.api;

public enum LensVisibility {
    COMPACT,
    EXPANDED,
    DEEP;

    public boolean visibleIn(LensScanMode mode) {
        LensScanMode safeMode = mode == null ? LensScanMode.COMPACT : mode;
        return switch (this) {
            case COMPACT -> true;
            case EXPANDED -> safeMode == LensScanMode.EXPANDED || safeMode == LensScanMode.DEEP;
            case DEEP -> safeMode == LensScanMode.DEEP;
        };
    }
}
