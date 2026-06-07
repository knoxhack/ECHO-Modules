package com.knoxhack.echolens.registry;

import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import java.util.List;
import net.minecraft.network.chat.Component;

final class LensReportSanitizer {
    private static final int MAX_SECTIONS = 12;
    private static final int MAX_ROWS = 24;
    private static final int MAX_TITLE = 96;
    private static final int MAX_TEXT = 160;
    private static final int MAX_ICON = 16;

    private LensReportSanitizer() {
    }

    static List<LensInfoSection> normalizeLocal(List<LensInfoSection> sections) {
        return normalize(sections, false);
    }

    static List<LensInfoSection> normalizeServer(List<LensInfoSection> sections) {
        return normalize(sections, true);
    }

    private static List<LensInfoSection> normalize(List<LensInfoSection> sections, boolean server) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }
        return sections.stream()
                .filter(section -> section != null)
                .limit(server ? MAX_SECTIONS : Long.MAX_VALUE)
                .map(section -> section(section, server))
                .filter(section -> !section.rows().isEmpty())
                .toList();
    }

    private static LensInfoSection section(LensInfoSection section, boolean server) {
        List<LensInfoRow> rows = section.rows().stream()
                .filter(row -> row != null)
                .limit(server ? MAX_ROWS : Long.MAX_VALUE)
                .map(row -> row(row, server))
                .toList();
        LensVisibility visibility = server ? LensVisibility.DEEP : section.visibility();
        return new LensInfoSection(
                section.id(),
                section.category() == null ? LensDataCategory.INTEGRATION : section.category(),
                Component.literal(clean(section.title().getString(), MAX_TITLE)),
                clean(section.icon(), MAX_ICON),
                section.tone() == null ? LensTone.NEUTRAL : section.tone(),
                visibility,
                rows);
    }

    private static LensInfoRow row(LensInfoRow row, boolean server) {
        return new LensInfoRow(
                Component.literal(clean(row.label().getString(), MAX_TEXT)),
                Component.literal(clean(row.value().getString(), MAX_TEXT)),
                clean(row.icon(), MAX_ICON),
                row.tone() == null ? LensTone.NEUTRAL : row.tone(),
                server ? LensVisibility.DEEP : row.visibility());
    }

    private static String clean(String value, int maxLength) {
        String clean = value == null ? "" : value.strip();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }
}
