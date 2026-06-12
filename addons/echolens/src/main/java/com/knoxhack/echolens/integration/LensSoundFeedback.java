package com.knoxhack.echolens.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.network.LensServerScanStatus;
import net.minecraft.resources.Identifier;

public final class LensSoundFeedback {
    public static final Identifier SCAN_START = EchoLens.id("sound/scan_start");
    public static final Identifier SCAN_VERIFIED = EchoLens.id("sound/scan_verified");
    public static final Identifier SCAN_REDACTED = EchoLens.id("sound/scan_redacted");
    public static final Identifier SCAN_UNAVAILABLE = EchoLens.id("sound/scan_unavailable");
    public static final Identifier ACTION_SHORTCUT = EchoLens.id("sound/action_shortcut");

    private LensSoundFeedback() {
    }

    public static void play(Identifier eventId) {
        if (eventId != null) {
            EchoCoreServices.soundService().playEvent(eventId);
        }
    }

    public static void playStatus(LensServerScanStatus status) {
        switch (status == null ? LensServerScanStatus.UNAVAILABLE : status) {
            case VERIFIED -> play(SCAN_VERIFIED);
            case REDACTED -> play(SCAN_REDACTED);
            case STALE, UNAVAILABLE -> play(SCAN_UNAVAILABLE);
            case LOCAL, QUERYING -> {
            }
        }
    }
}
