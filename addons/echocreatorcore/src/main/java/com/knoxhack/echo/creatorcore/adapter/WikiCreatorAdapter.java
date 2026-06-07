package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class WikiCreatorAdapter extends ModPresenceCreatorAdapter {
    public WikiCreatorAdapter() {
        super("wiki", "echowiki", "ECHO: Wiki", null,
                Set.of("preview"),
                "Wiki not installed; creator help panels use packaged docs only.",
                "Wiki detected; docs/help browser integration is reserved until a public article navigation hook is stable.",
                true);
    }
}
