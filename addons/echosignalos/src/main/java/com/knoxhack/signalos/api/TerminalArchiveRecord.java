package com.knoxhack.signalos.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * A lore, archive, or guide record shown in the Archive page.
 */
public record TerminalArchiveRecord(
        Identifier id,
        Identifier chapterId,
        String title,
        String group,
        String status,
        int order,
        List<String> lines,
        boolean locked) {
    public TerminalArchiveRecord {
        id = TerminalIds.requireLowercase(id, "Terminal archive");
        chapterId = TerminalIds.requireLowercase(chapterId, "Terminal archive chapter");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        group = group == null || group.isBlank() ? chapterId.getPath() : group.strip();
        status = status == null || status.isBlank() ? "OPEN" : status.strip().toUpperCase(java.util.Locale.ROOT);
        lines = List.copyOf(lines == null ? List.of() : lines);
    }

    public static Builder builder(String id) {
        return new Builder(TerminalIds.parse(id, "Terminal archive"));
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final Identifier id;
        private Identifier chapterId;
        private String title = "";
        private String group = "";
        private String status = "OPEN";
        private int order;
        private final List<String> lines = new ArrayList<>();
        private boolean locked;

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder chapter(String chapterId) {
            this.chapterId = TerminalIds.parse(chapterId, "Terminal archive chapter");
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder line(String line) {
            if (line != null && !line.isBlank()) {
                lines.add(line.strip());
            }
            return this;
        }

        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public TerminalArchiveRecord build() {
            return new TerminalArchiveRecord(id, chapterId, title, group, status, order, lines, locked);
        }
    }
}
