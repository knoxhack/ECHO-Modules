package com.knoxhack.echopresencelink.api;

public record EchoPresenceButton(String label, String url) {
    public EchoPresenceButton {
        label = PresenceSanitizer.text(label, 32, "");
        url = PresenceSanitizer.url(url);
    }

    public boolean valid() {
        return !label.isBlank() && !url.isBlank();
    }
}
