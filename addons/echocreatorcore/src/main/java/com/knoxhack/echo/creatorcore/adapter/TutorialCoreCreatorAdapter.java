package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class TutorialCoreCreatorAdapter extends ModPresenceCreatorAdapter {
    public TutorialCoreCreatorAdapter() {
        super("tutorialcore", "echotutorialcore", "ECHO: TutorialCore", null,
                Set.of("preview"),
                "TutorialCore not installed; tutorial_hint drafts remain generic JSON templates.",
                "TutorialCore detected; tutorial preview adapter is stubbed until a public creator API is available.",
                true);
    }
}
