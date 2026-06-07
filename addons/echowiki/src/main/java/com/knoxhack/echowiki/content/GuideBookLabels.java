package com.knoxhack.echowiki.content;

import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public final class GuideBookLabels {
    public static final Identifier DEFAULT_ICON = Identifier.fromNamespaceAndPath("minecraft", "written_book");

    private GuideBookLabels() {
    }

    public static String shortId(Identifier id) {
        if (id == null) {
            return "";
        }
        return "echowiki".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    public static MutableComponent moduleLabelComponent(String moduleId) {
        String normalized = normalizedModuleId(moduleId);
        return switch (normalized) {
            case "echoarcanacore", "echoarcaneindex", "echoashfallprotocol", "echoagriculturereclamation",
                    "echoarmory", "echoblackboxprotocol", "echoblockworks", "echoconvoyprotocol",
                    "echocursecore", "echogrimoire", "echoholomap", "echoindex", "echoindustrialnexus",
                    "echolens", "echologisticsnetwork", "echomultiblockcore", "echonexusprotocol",
                    "echoorbitalremnants", "echopowergrid", "echorecovery", "echorelictech",
                    "echoritualcore", "echospellcore", "signalos", "echostationfall", "echoterminal",
                    "echotutorialcore", "echoweathercore", "echowiki", "echoworldcore" ->
                    Component.translatable("module.echowiki." + normalized);
            default -> Component.literal(fallbackLabel(normalized));
        };
    }

    public static String moduleLabel(String moduleId) {
        return moduleLabelComponent(moduleId).getString();
    }

    public static MutableComponent chapterCountComponent(int count) {
        return Component.translatable(count == 1
                ? "text.echowiki.guide_book.chapter_count.one"
                : "text.echowiki.guide_book.chapter_count.many", count);
    }

    public static String chapterCountLabel(int count) {
        return chapterCountComponent(count).getString();
    }

    public static MutableComponent sectionCountComponent(int count) {
        return Component.translatable(count == 1
                ? "text.echowiki.guide_book.section_count.one"
                : "text.echowiki.guide_book.section_count.many", count);
    }

    public static String sectionCountLabel(int count) {
        return sectionCountComponent(count).getString();
    }

    public static MutableComponent chapterRoleComponent(Identifier articleId, Identifier homeArticleId, int index) {
        if (index == 0 || Objects.equals(articleId, homeArticleId)) {
            return Component.translatable("text.echowiki.guide_book.chapter_role.overview");
        }
        String role = roleKey(articleId);
        return switch (role) {
            case "first_steps", "core_loop", "systems", "progression", "integrations", "troubleshooting" ->
                    Component.translatable("text.echowiki.guide_book.chapter_role." + role);
            default -> Component.literal(fallbackLabel(role));
        };
    }

    public static String chapterRoleLabel(Identifier articleId, Identifier homeArticleId, int index) {
        return chapterRoleComponent(articleId, homeArticleId, index).getString();
    }

    public static MutableComponent chaptersAvailableComponent(int count) {
        return Component.translatable(count == 1
                ? "tooltip.echowiki.guide_book.chapters_available.one"
                : "tooltip.echowiki.guide_book.chapters_available.many", count);
    }

    public static MutableComponent availableManualsComponent(int count) {
        return Component.translatable(count == 1
                ? "tooltip.echowiki.guide_book.available_count.one"
                : "tooltip.echowiki.guide_book.available_count.many", count);
    }

    public static MutableComponent availabilityComponent(GuideBookDefinition guide) {
        if (GuideBookRegistry.isVisible(guide)) {
            return Component.translatable("text.echowiki.guide_book.availability.ready");
        }
        return Component.translatable("text.echowiki.guide_book.availability.requires",
                moduleLabelComponent(guide == null ? "" : guide.requiredModId()));
    }

    public static String availabilityLabel(GuideBookDefinition guide) {
        return availabilityComponent(guide).getString();
    }

    public static Identifier safeItemIcon(Identifier id) {
        return hasItemIcon(id) ? id : DEFAULT_ICON;
    }

    public static boolean hasItemIcon(Identifier id) {
        return id != null && BuiltInRegistries.ITEM.getOptional(id).isPresent();
    }

    private static String normalizedModuleId(String moduleId) {
        return moduleId == null ? "" : moduleId.toLowerCase(Locale.ROOT).strip();
    }

    private static String roleKey(Identifier articleId) {
        if (articleId == null) {
            return "";
        }
        String path = articleId.getPath();
        int slash = path.lastIndexOf('/');
        return (slash >= 0 ? path.substring(slash + 1) : path)
                .replace('-', '_')
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    private static String fallbackLabel(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return "ECHO";
        }
        String clean = moduleId.startsWith("echo") ? moduleId.substring(4) : moduleId;
        clean = clean.replace('-', '_').replace('.', '_');
        String[] parts = clean.split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? moduleId : builder.toString();
    }
}
