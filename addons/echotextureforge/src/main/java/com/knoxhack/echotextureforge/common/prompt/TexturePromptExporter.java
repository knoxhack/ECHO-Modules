package com.knoxhack.echotextureforge.common.prompt;

import com.knoxhack.echotextureforge.api.prompt.TexturePromptTemplate;
import com.knoxhack.echotextureforge.api.report.TextureAuditReport;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.common.export.TextureSheetExporter;
import com.knoxhack.echotextureforge.common.export.TextureSheetPlan;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TexturePromptExporter {
    private TexturePromptExporter() {
    }

    public static List<Path> export(TextureAuditReport report, TextureForgePaths paths) throws IOException {
        List<TextureSpec> missing = missingTextureSpecs(report);
        List<TextureSpec> missingItems = missing.stream().filter(TextureSpec::isItemLike).toList();
        List<TextureSpec> missingBlocks = missing.stream().filter(TextureSpec::isBlockLike).toList();
        List<TextureSpec> machineSpecs = missing.stream().filter(TexturePromptExporter::machineSpec).toList();
        List<TextureSpec> armorSpecs = missing.stream().filter(spec -> spec.assetKind() == TextureKind.ARMOR).toList();
        List<TextureSpec> uiSpecs = missing.stream().filter(spec -> spec.assetKind() == TextureKind.UI
                || spec.assetKind() == TextureKind.STRUCTURE_ICON).toList();
        List<TextureSpec> entitySpecs = missing.stream().filter(spec -> spec.assetKind() == TextureKind.ENTITY).toList();

        List<Path> files = new ArrayList<>();
        Path prompts = paths.promptsDir();
        files.add(writePromptFile(prompts.resolve("master_codex_texture_prompts.md"), "Master Codex Texture Prompts", missing));
        files.add(writePromptFile(prompts.resolve("codex_texture_prompts.md"), "Codex Texture Prompts", missing));
        files.add(writePromptFile(prompts.resolve("missing_item_textures.md"), "Missing Item Textures", missingItems));
        files.add(writePromptFile(prompts.resolve("missing_block_textures.md"), "Missing Block Textures", missingBlocks));

        files.add(writePromptFile(paths.promptTypeDir().resolve("item_textures.md"), "Item Texture Prompts", missingItems));
        files.add(writePromptFile(paths.promptTypeDir().resolve("block_textures.md"), "Block Texture Prompts", missingBlocks));
        files.add(writePromptFile(paths.promptTypeDir().resolve("machine_textures.md"), "Machine Texture Prompts", machineSpecs));
        files.add(writePromptFile(paths.promptTypeDir().resolve("armor_textures.md"), "Armor Texture Prompts", armorSpecs));
        files.add(writePromptFile(paths.promptTypeDir().resolve("entity_textures.md"), "Entity Texture Prompts", entitySpecs));
        files.add(writePromptFile(paths.promptTypeDir().resolve("ui_icons.md"), "UI Icon Prompts", uiSpecs));

        for (Map.Entry<String, List<TextureSpec>> entry : byNamespace(missing).entrySet()) {
            files.add(writePromptFile(paths.promptAddonDir().resolve(entry.getKey() + "_prompts.md"),
                    "Texture Prompts: " + entry.getKey(), entry.getValue()));
            files.add(writeSheet(paths.promptSheetsDir().resolve(entry.getKey() + "_sheet.md"),
                    paths.promptSheetsDir().resolve(entry.getKey() + "_sheet.cut_map.json"),
                    entry.getKey() + " texture sheet", "mixed", entry.getValue()));
            files.add(writeAddonSheet(paths, entry.getKey(), "item", entry.getValue(), TextureSpec::isItemLike));
            files.add(writeAddonSheet(paths, entry.getKey(), "block", entry.getValue(), TextureSpec::isBlockLike));
            files.add(writeAddonSheet(paths, entry.getKey(), "machine", entry.getValue(), TexturePromptExporter::machineSpec));
            files.add(writeAddonSheet(paths, entry.getKey(), "ui", entry.getValue(), spec -> spec.assetKind() == TextureKind.UI
                    || spec.assetKind() == TextureKind.STRUCTURE_ICON));
        }

        files.add(writeSheet(prompts.resolve("machine_texture_sheets.md"), prompts.resolve("machine_texture_sheets.cut_map.json"),
                "Machine Texture Sheets", "machine", machineSpecs));
        files.add(writeSheet(prompts.resolve("ui_icon_sheets.md"), prompts.resolve("ui_icon_sheets.cut_map.json"),
                "UI Icon Sheets", "ui", uiSpecs));
        files.add(writeSheet(prompts.resolve("entity_texture_sheets.md"), prompts.resolve("entity_texture_sheets.cut_map.json"),
                "Entity Texture Sheets", "entity", entitySpecs));
        return files.stream().distinct().toList();
    }

    private static List<TextureSpec> missingTextureSpecs(TextureAuditReport report) {
        Set<String> missingKeys = report.issues().stream()
                .filter(issue -> "MISSING_TEXTURE".equals(issue.code()) || "WRONG_TEXTURE_SIZE".equals(issue.code())
                        || "MISSING_TRANSPARENT_BACKGROUND".equals(issue.code()))
                .map(issue -> issue.namespace() + ":" + issue.assetId())
                .collect(Collectors.toSet());
        return report.specs().stream()
                .filter(spec -> missingKeys.contains(spec.namespace() + ":" + spec.assetId()))
                .sorted(Comparator.comparingInt(TextureSpec::promptPriority).reversed()
                        .thenComparing(TextureSpec::namespace)
                        .thenComparing(spec -> spec.assetKind().id())
                        .thenComparing(TextureSpec::assetId))
                .toList();
    }

    private static Map<String, List<TextureSpec>> byNamespace(List<TextureSpec> specs) {
        return specs.stream().collect(Collectors.groupingBy(TextureSpec::namespace, java.util.TreeMap::new, Collectors.toList()));
    }

    private static Path writePromptFile(Path path, String title, List<TextureSpec> specs) throws IOException {
        StringBuilder out = new StringBuilder(TextureForgeMarkdown.heading(title));
        if (specs.isEmpty()) {
            out.append("No texture prompts for this category.\n");
        } else {
            for (TextureSpec spec : specs) {
                out.append("## ").append(spec.namespace()).append(':').append(spec.assetId()).append("\n\n");
                out.append(TextureForgeMarkdown.codeFence("text", TexturePromptTemplate.singleTexturePrompt(spec)));
                out.append('\n');
            }
        }
        TextureForgeMarkdown.write(path, out.toString());
        return path;
    }

    private static Path writeAddonSheet(TextureForgePaths paths, String namespace, String type,
                                        List<TextureSpec> specs, Predicate<TextureSpec> filter) throws IOException {
        List<TextureSpec> filtered = specs.stream().filter(filter).toList();
        Path md = paths.promptSheetsDir().resolve(namespace + "_" + type + "_sheet.md");
        Path json = paths.promptSheetsDir().resolve(namespace + "_" + type + "_sheet.cut_map.json");
        return writeSheet(md, json, namespace + " " + type + " texture sheet", type, filtered);
    }

    private static Path writeSheet(Path markdownPath, Path jsonPath, String title, String sheetType,
                                   List<TextureSpec> specs) throws IOException {
        TextureSheetPlan plan = TextureSheetExporter.plan(slug(title), sheetType, specs);
        TextureSheetExporter.writePlan(markdownPath, jsonPath, plan);
        return markdownPath;
    }

    private static boolean machineSpec(TextureSpec spec) {
        return spec.assetKind() == TextureKind.MACHINE || "machine".equals(spec.sheetGroup());
    }

    private static String slug(String value) {
        return value == null ? "texture_sheet" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_ -]", "")
                .replace(' ', '_');
    }
}
