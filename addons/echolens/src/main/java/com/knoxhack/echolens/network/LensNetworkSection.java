package com.knoxhack.echolens.network;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record LensNetworkSection(
        Identifier id,
        LensDataCategory category,
        String title,
        String icon,
        LensTone tone,
        LensVisibility visibility,
        List<LensNetworkRow> rows) {
    private static final int MAX_ID = 192;
    private static final int MAX_TITLE = 96;
    private static final int MAX_ICON = 16;
    private static final int MAX_ROWS = 24;

    public LensNetworkSection {
        id = id == null ? EchoLens.id("server/unknown") : id;
        category = category == null ? LensDataCategory.INTEGRATION : category;
        title = safe(title, MAX_TITLE);
        icon = safe(icon == null || icon.isBlank() ? "S" : icon, MAX_ICON);
        tone = tone == null ? LensTone.INFO : tone;
        visibility = visibility == null ? LensVisibility.DEEP : visibility;
        rows = List.copyOf(rows == null ? List.of() : rows.stream()
                .filter(row -> row != null)
                .limit(MAX_ROWS)
                .toList());
    }

    public static LensNetworkSection from(LensInfoSection section) {
        return new LensNetworkSection(
                section.id(),
                section.category(),
                section.title().getString(),
                section.icon(),
                section.tone(),
                section.visibility(),
                section.rows().stream().map(LensNetworkRow::from).toList());
    }

    public LensInfoSection toSection() {
        return new LensInfoSection(
                id,
                category,
                Component.literal(title),
                icon,
                tone,
                visibility,
                rows.stream().map(LensNetworkRow::toRow).toList());
    }

    static void write(FriendlyByteBuf buffer, LensNetworkSection section) {
        buffer.writeUtf(section.id().toString(), MAX_ID);
        buffer.writeEnum(section.category());
        buffer.writeUtf(section.title(), MAX_TITLE);
        buffer.writeUtf(section.icon(), MAX_ICON);
        buffer.writeEnum(section.tone());
        buffer.writeEnum(section.visibility());
        buffer.writeVarInt(section.rows().size());
        for (LensNetworkRow row : section.rows()) {
            LensNetworkRow.write(buffer, row);
        }
    }

    static LensNetworkSection read(FriendlyByteBuf buffer) {
        Identifier id = Identifier.tryParse(buffer.readUtf(MAX_ID));
        LensDataCategory category = buffer.readEnum(LensDataCategory.class);
        String title = buffer.readUtf(MAX_TITLE);
        String icon = buffer.readUtf(MAX_ICON);
        LensTone tone = buffer.readEnum(LensTone.class);
        LensVisibility visibility = buffer.readEnum(LensVisibility.class);
        int count = Math.max(0, Math.min(MAX_ROWS, buffer.readVarInt()));
        java.util.ArrayList<LensNetworkRow> rows = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(LensNetworkRow.read(buffer));
        }
        return new LensNetworkSection(id, category, title, icon, tone, visibility, rows);
    }

    private static String safe(String value, int maxLength) {
        String safe = value == null ? "" : value.strip();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
