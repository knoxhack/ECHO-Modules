package com.knoxhack.echotextureforge.common.review;

import com.knoxhack.echotextureforge.EchoTextureForgeMod;
import com.knoxhack.echotextureforge.common.util.TextureForgeJson;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class TextureReviewService {
    private TextureReviewService() {
    }

    public static Path statePath(TextureForgePaths paths) {
        return paths.reviewDir().resolve("review_state.json");
    }

    public static TextureReviewState load(TextureForgePaths paths) {
        Path path = statePath(paths);
        if (!Files.isRegularFile(path)) {
            return new TextureReviewState(Instant.now(), List.of());
        }
        try {
            TextureReviewState state = TextureForgeJson.read(path, TextureReviewState.class);
            return state == null ? new TextureReviewState(Instant.now(), List.of()) : state;
        } catch (IOException | RuntimeException exception) {
            EchoTextureForgeMod.LOGGER.warn("TextureForge could not read review state {}.", path, exception);
            return new TextureReviewState(Instant.now(), List.of());
        }
    }

    public static TextureReviewState upsertAll(TextureForgePaths paths, List<TextureReviewEntry> incoming) throws IOException {
        TextureReviewState existing = load(paths);
        List<TextureReviewEntry> entries = new ArrayList<>(existing.entries());
        for (TextureReviewEntry entry : incoming) {
            int index = indexOf(entries, entry.specId(), entry.targetOutputPath());
            if (index >= 0) {
                TextureReviewEntry current = entries.get(index);
                if (current.status() == TextureReviewStatus.PENDING) {
                    entries.set(index, entry);
                }
            } else {
                entries.add(entry);
            }
        }
        return save(paths, entries);
    }

    public static TextureReviewState update(TextureForgePaths paths, String asset, TextureReviewStatus status,
                                            String notes) throws IOException {
        TextureReviewState existing = load(paths);
        List<TextureReviewEntry> entries = new ArrayList<>();
        boolean changed = false;
        for (TextureReviewEntry entry : existing.entries()) {
            if (matches(entry, asset)) {
                entries.add(new TextureReviewEntry(entry.specId(), entry.generatedFilePath(), entry.targetOutputPath(),
                        status, notes == null || notes.isBlank() ? entry.notes() : notes, Instant.now().toString(),
                        entry.sourceSheet(), entry.sourcePrompt()));
                changed = true;
            } else {
                entries.add(entry);
            }
        }
        if (!changed) {
            entries.add(new TextureReviewEntry(asset, "", asset, status, notes, Instant.now().toString(), "", ""));
        }
        return save(paths, entries);
    }

    public static boolean isApproved(TextureForgePaths paths, String namespace, String relativePath) {
        String target = "assets/" + namespace + "/" + relativePath.replace('\\', '/');
        Optional<TextureReviewEntry> entry = load(paths).entries().stream()
                .filter(candidate -> candidate.targetOutputPath().equals(target)
                        || candidate.specId().equals(namespace + ":" + stripAssetId(relativePath)))
                .findFirst();
        return entry.map(value -> value.status() == TextureReviewStatus.APPROVED
                || value.status() == TextureReviewStatus.APPLIED).orElse(false);
    }

    public static Path exportMarkdown(TextureForgePaths paths) throws IOException {
        TextureReviewState state = load(paths);
        Path path = paths.reviewDir().resolve("review_state.md");
        StringBuilder out = new StringBuilder(TextureForgeMarkdown.heading("TextureForge Review State"));
        if (state.entries().isEmpty()) {
            out.append("No generated assets are pending review.\n");
        } else {
            out.append("| Status | Asset | Generated File | Target | Notes |\n");
            out.append("| --- | --- | --- | --- | --- |\n");
            state.entries().stream()
                    .sorted(Comparator.comparing(TextureReviewEntry::status).thenComparing(TextureReviewEntry::specId))
                    .forEach(entry -> out.append("| ").append(entry.status().id())
                            .append(" | `").append(entry.specId()).append("`")
                            .append(" | `").append(entry.generatedFilePath()).append("`")
                            .append(" | `").append(entry.targetOutputPath()).append("`")
                            .append(" | ").append(entry.notes().replace("|", "\\|")).append(" |\n"));
        }
        TextureForgeMarkdown.write(path, out.toString());
        return path;
    }

    public static TextureReviewState markApplied(TextureForgePaths paths, String namespace, String relativePath) throws IOException {
        return update(paths, namespace + ":" + stripAssetId(relativePath), TextureReviewStatus.APPLIED, "Applied to source assets.");
    }

    private static TextureReviewState save(TextureForgePaths paths, List<TextureReviewEntry> entries) throws IOException {
        TextureReviewState state = new TextureReviewState(Instant.now(), entries);
        TextureForgeJson.write(statePath(paths), state);
        exportMarkdown(paths);
        return state;
    }

    private static int indexOf(List<TextureReviewEntry> entries, String specId, String targetOutputPath) {
        for (int i = 0; i < entries.size(); i++) {
            TextureReviewEntry entry = entries.get(i);
            if (entry.specId().equals(specId) || entry.targetOutputPath().equals(targetOutputPath)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matches(TextureReviewEntry entry, String asset) {
        if (asset == null || asset.isBlank()) {
            return false;
        }
        String clean = asset.strip();
        return entry.specId().equals(clean)
                || entry.targetOutputPath().equals(clean)
                || entry.targetOutputPath().endsWith("/" + clean)
                || entry.generatedFilePath().endsWith("/" + clean);
    }

    private static String stripAssetId(String relativePath) {
        String value = relativePath.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }
}
