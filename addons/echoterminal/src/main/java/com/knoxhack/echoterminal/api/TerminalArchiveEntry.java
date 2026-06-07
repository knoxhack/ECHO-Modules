package com.knoxhack.echoterminal.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record TerminalArchiveEntry(
        Identifier id,
        String group,
        String title,
        String status,
        List<String> lines,
        boolean locked) {
    public TerminalArchiveEntry {
        TerminalApiIds.requireLowercase(id, "Terminal archive entry");
        lines = List.copyOf(lines == null ? List.of() : lines);
    }
}
