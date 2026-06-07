package com.knoxhack.echotutorialcore.util;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;

public final class TutorialDebug {
    private TutorialDebug() {}

    public static void printStatus() {
        EchoTutorialCore.LOGGER.info("TutorialCore Status: {} cards, {} hints, {} flows registered.",
                TutorialCoreRegistries.cardCount(),
                TutorialCoreRegistries.hintCount(),
                TutorialCoreRegistries.flowCount());
    }
}
