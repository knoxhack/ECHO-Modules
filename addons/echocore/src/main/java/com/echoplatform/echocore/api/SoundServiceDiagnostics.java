package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record SoundServiceDiagnostics(
        boolean available,
        Identifier currentTrack,
        String priority,
        String selectionReason,
        int musicProfileCount,
        int ambienceProfileCount,
        List<Identifier> activeEvents,
        List<String> missingAssets,
        String lastFailure) {
    public SoundServiceDiagnostics {
        priority = priority == null ? "" : priority;
        selectionReason = selectionReason == null ? "" : selectionReason;
        activeEvents = activeEvents == null ? List.of() : List.copyOf(activeEvents);
        missingAssets = missingAssets == null ? List.of() : List.copyOf(missingAssets);
        lastFailure = lastFailure == null ? "" : lastFailure;
    }

    public static SoundServiceDiagnostics unavailable() {
        return new SoundServiceDiagnostics(false, null, "", "", 0, 0, List.of(), List.of(), "");
    }
}
