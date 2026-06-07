package com.knoxhack.echolens.network;

import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public record LensNetworkRow(
        String label,
        String value,
        String icon,
        LensTone tone,
        LensVisibility visibility) {
    private static final int MAX_TEXT = 160;
    private static final int MAX_ICON = 16;

    public LensNetworkRow {
        label = safe(label, MAX_TEXT);
        value = safe(value, MAX_TEXT);
        icon = safe(icon == null || icon.isBlank() ? "-" : icon, MAX_ICON);
        tone = tone == null ? LensTone.NEUTRAL : tone;
        visibility = visibility == null ? LensVisibility.DEEP : visibility;
    }

    public static LensNetworkRow from(LensInfoRow row) {
        return new LensNetworkRow(
                row.label().getString(),
                row.value().getString(),
                row.icon(),
                row.tone(),
                row.visibility());
    }

    public LensInfoRow toRow() {
        return new LensInfoRow(Component.literal(label), Component.literal(value), icon, tone, visibility);
    }

    static void write(FriendlyByteBuf buffer, LensNetworkRow row) {
        buffer.writeUtf(row.label(), MAX_TEXT);
        buffer.writeUtf(row.value(), MAX_TEXT);
        buffer.writeUtf(row.icon(), MAX_ICON);
        buffer.writeEnum(row.tone());
        buffer.writeEnum(row.visibility());
    }

    static LensNetworkRow read(FriendlyByteBuf buffer) {
        return new LensNetworkRow(
                buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_ICON),
                buffer.readEnum(LensTone.class),
                buffer.readEnum(LensVisibility.class));
    }

    private static String safe(String value, int maxLength) {
        String safe = value == null ? "" : value.strip();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
