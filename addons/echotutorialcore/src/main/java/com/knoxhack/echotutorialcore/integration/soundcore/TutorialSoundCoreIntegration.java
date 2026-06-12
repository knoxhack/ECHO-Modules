package com.knoxhack.echotutorialcore.integration.soundcore;

import com.echoplatform.echocore.api.EchoOptionalServices;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialHintType;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import net.minecraft.resources.Identifier;

public final class TutorialSoundCoreIntegration {
    private TutorialSoundCoreIntegration() {}

    public static void register() {
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with SoundCore. Tutorial sound event bridge registered.");
    }

    public static void playHint(TutorialHintType type) {
        if (!tutorialSoundsEnabled()) {
            return;
        }
        Identifier event = type == TutorialHintType.DANGER || type == TutorialHintType.HAZARD_HELP
                ? Identifier.fromNamespaceAndPath("echosoundcore", "ui.terminal.warning")
                : Identifier.fromNamespaceAndPath("echosoundcore", "ui.terminal.new_intel");
        EchoOptionalServices.soundCoreOrNoOp().playEvent(event);
    }

    public static void playCardUnlock() {
        if (tutorialSoundsEnabled()) {
            EchoOptionalServices.soundCoreOrNoOp().playEvent(
                    Identifier.fromNamespaceAndPath("echosoundcore", "stinger.chapter.unlocked"));
        }
    }

    private static boolean tutorialSoundsEnabled() {
        try {
            return TutorialConfig.PLAY_TUTORIAL_SOUNDS.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
