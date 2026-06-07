package com.knoxhack.echomultiblockcore.api;

public record AutoBuilderResult(boolean success, int placedBlocks, String message) {
    public AutoBuilderResult {
        placedBlocks = Math.max(0, placedBlocks);
        message = message == null || message.isBlank() ? (success ? "Auto-builder complete." : "Auto-builder blocked.") : message.strip();
    }

    public static AutoBuilderResult success(int placedBlocks, String message) {
        return new AutoBuilderResult(true, placedBlocks, message);
    }

    public static AutoBuilderResult blocked(String message) {
        return new AutoBuilderResult(false, 0, message);
    }
}
