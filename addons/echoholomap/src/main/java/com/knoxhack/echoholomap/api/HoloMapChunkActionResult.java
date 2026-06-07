package com.knoxhack.echoholomap.api;

public record HoloMapChunkActionResult(boolean success, String title, String message) {
    public HoloMapChunkActionResult {
        title = title == null ? "" : title.strip();
        message = message == null ? "" : message.strip();
    }

    public static HoloMapChunkActionResult success(String title, String message) {
        return new HoloMapChunkActionResult(true, title, message);
    }

    public static HoloMapChunkActionResult failure(String title, String message) {
        return new HoloMapChunkActionResult(false, title, message);
    }

    public String statusLine() {
        if (title.isBlank()) {
            return message;
        }
        if (message.isBlank()) {
            return title;
        }
        return title + ": " + message;
    }
}
