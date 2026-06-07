package com.knoxhack.signalos.api;

import net.minecraft.resources.Identifier;

/**
 * Optional metadata for custom page surfaces rendered by the SignalOS desktop shell.
 */
public record TerminalPage(
        Identifier id,
        Identifier chapterId,
        String title,
        String type,
        int order) {
    public TerminalPage {
        id = TerminalIds.requireLowercase(id, "Terminal page");
        chapterId = TerminalIds.requireLowercase(chapterId, "Terminal page chapter");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        type = type == null || type.isBlank() ? "custom" : type.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public static Builder builder(String id) {
        return new Builder(TerminalIds.parse(id, "Terminal page"));
    }

    public static final class Builder {
        private final Identifier id;
        private Identifier chapterId;
        private String title = "";
        private String type = "custom";
        private int order;

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder chapter(String chapterId) {
            this.chapterId = TerminalIds.parse(chapterId, "Terminal page chapter");
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public TerminalPage build() {
            return new TerminalPage(id, chapterId, title, type, order);
        }
    }
}
