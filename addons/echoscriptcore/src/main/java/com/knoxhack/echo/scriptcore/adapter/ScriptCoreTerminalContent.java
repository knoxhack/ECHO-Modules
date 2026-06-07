package com.knoxhack.echo.scriptcore.adapter;

import com.knoxhack.echo.scriptcore.model.EchoArchiveEntryDefinition;
import com.knoxhack.echo.scriptcore.api.EchoScriptRegistryView;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import java.util.ArrayList;
import java.util.List;

final class ScriptCoreTerminalContent {
    static final String SOURCE = "echoscriptcore";

    private ScriptCoreTerminalContent() {
    }

    static void registerArchives(EchoScriptRegistryView registry) {
        if (registry == null) {
            TerminalArchiveRegistry.replaceSource(SOURCE, List.of());
            return;
        }
        List<TerminalArchiveEntry> entries = new ArrayList<>();
        for (var definition : registry.getByType("archive_entry")) {
            if (!(definition instanceof EchoArchiveEntryDefinition archive)) {
                continue;
            }
            List<String> lines = new ArrayList<>();
            if (!archive.subtitle().isBlank()) {
                lines.add(archive.subtitle());
            }
            lines.addAll(archive.content());
            entries.add(new TerminalArchiveEntry(
                    archive.id(),
                    archive.category().isBlank() ? archive.pack() : archive.category(),
                    archive.title().orElse(archive.id().toString()),
                    archive.importance(),
                    lines,
                    false));
        }
        TerminalArchiveRegistry.replaceSource(SOURCE, entries);
    }
}
