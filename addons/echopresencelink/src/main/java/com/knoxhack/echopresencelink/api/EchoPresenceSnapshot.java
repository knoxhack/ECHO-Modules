package com.knoxhack.echopresencelink.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoPresenceSnapshot(
        Identifier id,
        int priority,
        String details,
        String state,
        String largeImageKey,
        String largeImageText,
        String smallImageKey,
        String smallImageText,
        long startTimestamp,
        List<EchoPresenceButton> buttons,
        boolean clear) {
    public EchoPresenceSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("Presence snapshot id is required.");
        }
        priority = Math.max(0, priority);
        details = PresenceSanitizer.text(details, 128, "ECHO Presence Link");
        state = PresenceSanitizer.text(state, 128, "");
        largeImageKey = PresenceSanitizer.assetKey(largeImageKey, "echo_ashfall");
        largeImageText = PresenceSanitizer.text(largeImageText, 128, details);
        smallImageKey = PresenceSanitizer.assetKey(smallImageKey, "");
        smallImageText = PresenceSanitizer.text(smallImageText, 128, "");
        startTimestamp = Math.max(0L, startTimestamp);
        buttons = List.copyOf(buttons == null ? List.of() : buttons.stream()
                .filter(button -> button != null && button.valid())
                .limit(2)
                .toList());
    }

    public static EchoPresenceSnapshot of(Identifier id, int priority, String details, String state,
            String largeImageKey, long startTimestamp) {
        return new EchoPresenceSnapshot(id, priority, details, state, largeImageKey, details,
                "", "", startTimestamp, List.of(), false);
    }

    public EchoPresenceSnapshot withButtons(List<EchoPresenceButton> nextButtons) {
        return new EchoPresenceSnapshot(id, priority, details, state, largeImageKey, largeImageText,
                smallImageKey, smallImageText, startTimestamp, nextButtons, clear);
    }

    public EchoPresenceSnapshot withSmallImage(String key, String text) {
        return new EchoPresenceSnapshot(id, priority, details, state, largeImageKey, largeImageText,
                key, text, startTimestamp, buttons, clear);
    }

    public static EchoPresenceSnapshot clear(Identifier id) {
        return new EchoPresenceSnapshot(id, 0, "ECHO Presence Link", "", "echo_ashfall",
                "", "", "", 0L, List.of(), true);
    }
}
