package com.knoxhack.echobasegrid.api;

public record ClaimActionResult(boolean success, String title, String message) {
    public ClaimActionResult {
        title = title == null ? "" : title;
        message = message == null ? "" : message;
    }

    public static ClaimActionResult success(String title, String message) {
        return new ClaimActionResult(true, title, message);
    }

    public static ClaimActionResult failure(String title, String message) {
        return new ClaimActionResult(false, title, message);
    }
}
