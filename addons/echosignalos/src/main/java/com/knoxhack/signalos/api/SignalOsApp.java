package com.knoxhack.signalos.api;

import java.util.Locale;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Metadata for a SignalOS desktop app. Client renderers can use the app type to
 * choose a built-in surface while addons can still register shell-visible app
 * entries through Java or JSON.
 */
public record SignalOsApp(
        Identifier id,
        String title,
        String type,
        String summary,
        int order,
        int accentColor,
        Identifier icon,
        String permission,
        String view,
        List<String> recordTypes,
        List<String> recordSources,
        boolean includeArchived,
        String emptyText) {
    public SignalOsApp {
        id = TerminalIds.requireLowercase(id, "SignalOS app");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        type = clean(type, "custom");
        summary = summary == null ? "" : summary.strip();
        accentColor = opaque(accentColor == 0 ? 0x66E8FF : accentColor);
        if (icon != null) {
            icon = TerminalIds.requireLowercase(icon, "SignalOS app icon");
        }
        permission = clean(permission, "user");
        view = clean(view, "");
        recordTypes = cleanList(recordTypes);
        recordSources = cleanList(recordSources);
        emptyText = emptyText == null || emptyText.isBlank() ? "NO RECORDS AVAILABLE" : emptyText.strip();
    }

    public SignalOsApp(Identifier id, String title, String type, String summary, int order, int accentColor,
            Identifier icon, String permission) {
        this(id, title, type, summary, order, accentColor, icon, permission, "", List.of(), List.of(), false,
                "NO RECORDS AVAILABLE");
    }

    public static Builder builder(String id) {
        return new Builder(TerminalIds.parse(id, "SignalOS app"));
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.strip().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static int opaque(int color) {
        return (color >>> 24) == 0 ? 0xFF000000 | color : color;
    }

    public static final class Builder {
        private final Identifier id;
        private String title = "";
        private String type = "custom";
        private String summary = "";
        private int order;
        private int accentColor = 0xFF66E8FF;
        private Identifier icon;
        private String permission = "user";
        private String view = "";
        private List<String> recordTypes = List.of();
        private List<String> recordSources = List.of();
        private boolean includeArchived;
        private String emptyText = "NO RECORDS AVAILABLE";

        private Builder(Identifier id) {
            this.id = TerminalIds.requireLowercase(id, "SignalOS app");
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon == null || icon.isBlank() ? null : TerminalIds.parse(icon, "SignalOS app icon");
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder view(String view) {
            this.view = view;
            return this;
        }

        public Builder recordTypes(List<String> recordTypes) {
            this.recordTypes = recordTypes == null ? List.of() : List.copyOf(recordTypes);
            return this;
        }

        public Builder recordSources(List<String> recordSources) {
            this.recordSources = recordSources == null ? List.of() : List.copyOf(recordSources);
            return this;
        }

        public Builder includeArchived(boolean includeArchived) {
            this.includeArchived = includeArchived;
            return this;
        }

        public Builder emptyText(String emptyText) {
            this.emptyText = emptyText;
            return this;
        }

        public SignalOsApp build() {
            return new SignalOsApp(id, title, type, summary, order, accentColor, icon, permission, view,
                    recordTypes, recordSources, includeArchived, emptyText);
        }
    }
}
