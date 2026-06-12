package com.echoplatform.echocore.api;

public record EchoFactionActionResult(boolean success, String title, String message) {
    public EchoFactionActionResult {
        title = title == null ? "" : title;
        message = message == null ? "" : message;
    }

    public static EchoFactionActionResult success(String title, String message) {
        return new EchoFactionActionResult(true, title, message);
    }

    public static EchoFactionActionResult info(String title, String message) {
        return new EchoFactionActionResult(true, title, message);
    }

    public static EchoFactionActionResult failure(String title, String message) {
        return new EchoFactionActionResult(false, title, message);
    }

    public boolean refresh() {
        return success;
    }
}
