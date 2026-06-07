package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echorendercore.profile.ProfileValidationReport;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class IndexRenderCorePreviewBridge {
    private static final int ACCENT = 0xFF66E8FF;
    private static final int OK = 0xFF92F7A6;
    private static final int WARN = 0xFFFFD166;
    private static final int ERROR = 0xFFFF8FA3;

    private IndexRenderCorePreviewBridge() {
    }

    public static int drawPreview(TerminalRenderContext context, GuiGraphicsExtractor graphics, IndexEntry entry,
            int x, int y, int w, int mouseX, int mouseY) {
        ResolvedProfile resolved = resolve(entry);
        if (resolved == null) {
            return y;
        }
        VisualProfile profile = resolved.profile();
        int h = 78;
        TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
        TerminalUi.itemSlot(context, graphics, entry.icon(), x + 10, y + 12, ACCENT,
                TerminalUi.inside(mouseX, mouseY, x + 10, y + 12, 20, 20));
        TerminalUi.line(context, graphics, "RENDERCORE PREVIEW", x + 38, y + 10, w - 48, TerminalUi.accent(context));
        TerminalUi.line(context, graphics, resolved.id().toString(), x + 38, y + 23, w - 48, TerminalUi.text(context));

        String animation = profile.animationProfile() == null ? "none" : profile.animationProfile().toString();
        String particles = profile.particleProfile() == null ? "none" : profile.particleProfile().toString();
        TerminalUi.line(context, graphics,
                "state " + profile.defaultState().serializedName()
                        + " / layers " + profile.layers().size()
                        + " / anchors " + profile.anchors().size(),
                x + 12, y + 44, w - 24, TerminalUi.muted(context));
        TerminalUi.line(context, graphics,
                "anim " + trim(animation, 34) + " / particles " + trim(particles, 34),
                x + 12, y + 56, w - 24, TerminalUi.muted(context));

        ProfileValidationReport report = RenderCoreProfiles.loaded().validationReport()
                .forNamespace(resolved.id().getNamespace());
        int statusColor = report.errors() > 0 ? ERROR : report.warnings() > 0 ? WARN : OK;
        TerminalUi.line(context, graphics, report.summaryLine(), x + Math.max(94, w - 132), y + 10, 120, statusColor);
        return y + h + 8;
    }

    private static ResolvedProfile resolve(IndexEntry entry) {
        if (entry == null) {
            return null;
        }
        for (Identifier candidate : candidates(entry)) {
            VisualProfile profile = RenderCoreProfiles.visual(candidate);
            if (profile != null) {
                return new ResolvedProfile(candidate, profile);
            }
        }
        return null;
    }

    private static List<Identifier> candidates(IndexEntry entry) {
        List<Identifier> ids = new ArrayList<>();
        for (String tag : entry.tags()) {
            if (tag != null && tag.startsWith("rendercore=")) {
                parse(tag.substring("rendercore=".length())).ifPresent(ids::add);
            }
        }
        ids.add(entry.id());
        ids.addAll(entry.linkedItems());
        ItemStack icon = entry.icon();
        if (!icon.isEmpty()) {
            Identifier iconId = BuiltInRegistries.ITEM.getKey(icon.getItem());
            if (iconId != null) {
                ids.add(iconId);
            }
        }
        return ids.stream().distinct().toList();
    }

    private static java.util.Optional<Identifier> parse(String value) {
        try {
            return value == null || value.isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(Identifier.parse(value));
        } catch (RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private static String trim(String value, int max) {
        String safe = value == null ? "" : value;
        return safe.length() <= max ? safe : safe.substring(0, Math.max(0, max - 3)) + "...";
    }

    private record ResolvedProfile(Identifier id, VisualProfile profile) {
    }
}
