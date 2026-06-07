package com.knoxhack.echo.creatorcore.adapter;

import java.util.Set;

public final class TerminalCreatorAdapter extends ModPresenceCreatorAdapter {
    public TerminalCreatorAdapter() {
        super("terminal", "echoterminal", "ECHO: Terminal", null,
                Set.of("terminal_entry", "preview"),
                "Terminal not installed; use /echo creatorcore open or the CreatorCore keybind.",
                "Terminal detected; CreatorCore registers a client Terminal tab and addon summary card when the client loads.",
                true);
    }
}
