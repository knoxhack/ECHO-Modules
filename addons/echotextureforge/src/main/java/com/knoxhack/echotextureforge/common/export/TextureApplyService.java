package com.knoxhack.echotextureforge.common.export;

import com.knoxhack.echotextureforge.common.review.TextureReviewService;
import com.knoxhack.echotextureforge.common.util.TextureForgeJson;
import com.knoxhack.echotextureforge.common.util.TextureForgeMarkdown;
import com.knoxhack.echotextureforge.common.util.TextureForgePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class TextureApplyService {
    private TextureApplyService() {
    }

    public static TextureApplyResult apply(TextureForgePaths paths, String modidFilter,
                                           boolean dryRun, boolean overwriteApproved) throws IOException {
        String filter = modidFilter == null ? "" : modidFilter.strip().toLowerCase(Locale.ROOT);
        List<TextureApplyAction> actions = new ArrayList<>();
        List<String> backups = new ArrayList<>();
        int copied = 0;
        int skipped = 0;
        int conflicts = 0;

        if (!Files.isDirectory(paths.importStagedDir())) {
            TextureApplyResult result = new TextureApplyResult(Instant.now(), dryRun, overwriteApproved, filter,
                    0, 0, 0, List.of(), List.of(new TextureApplyAction("", "", "", "",
                    "skipped", "No staged import directory exists.")));
            writeReport(paths, result);
            return result;
        }

        List<Path> stagedFiles;
        try (Stream<Path> stream = Files.walk(paths.importStagedDir())) {
            stagedFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }

        for (Path staged : stagedFiles) {
            Path relative = paths.importStagedDir().relativize(staged);
            if (relative.getNameCount() < 2) {
                skipped++;
                actions.add(new TextureApplyAction("", relative.toString(), staged.toString(), "",
                        "skipped", "Staged path does not include a namespace segment."));
                continue;
            }
            String namespace = relative.getName(0).toString().toLowerCase(Locale.ROOT);
            if (!filter.isBlank() && !filter.equals(namespace)) {
                skipped++;
                actions.add(new TextureApplyAction(namespace, relative.toString(), staged.toString(), "",
                        "skipped", "Skipped by modid filter."));
                continue;
            }
            String relativeAsset = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
            Path target = paths.sourceAssetPath(namespace, relativeAsset);
            boolean targetExists = Files.exists(target);
            boolean approved = TextureReviewService.isApproved(paths, namespace, relativeAsset);
            if (targetExists && (!overwriteApproved || !approved)) {
                conflicts++;
                actions.add(new TextureApplyAction(namespace, relativeAsset, staged.toString(), target.toString(),
                        "conflict", "Target exists; default apply never overwrites existing textures."));
                continue;
            }
            if (dryRun) {
                copied++;
                actions.add(new TextureApplyAction(namespace, relativeAsset, staged.toString(), target.toString(),
                        targetExists ? "would_overwrite_approved" : "would_copy",
                        targetExists ? "Approved overwrite would copy this file." : "Missing target would be copied."));
                continue;
            }
            Files.createDirectories(target.getParent());
            if (targetExists) {
                Path backup = backupPath(paths, namespace, relativeAsset);
                Files.createDirectories(backup.getParent());
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
                backups.add(backup.toString());
            }
            Files.copy(staged, target, StandardCopyOption.REPLACE_EXISTING);
            TextureReviewService.markApplied(paths, namespace, relativeAsset);
            copied++;
            actions.add(new TextureApplyAction(namespace, relativeAsset, staged.toString(), target.toString(),
                    targetExists ? "overwritten_approved" : "copied",
                    targetExists ? "Copied with approved overwrite and backup." : "Copied missing texture into source assets."));
        }

        TextureApplyResult result = new TextureApplyResult(Instant.now(), dryRun, overwriteApproved, filter,
                copied, skipped, conflicts, backups, actions);
        writeReport(paths, result);
        return result;
    }

    private static Path backupPath(TextureForgePaths paths, String namespace, String relativeAsset) {
        String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
        return paths.importDir().resolve("backups").resolve(stamp).resolve(namespace).resolve(relativeAsset);
    }

    private static void writeReport(TextureForgePaths paths, TextureApplyResult result) throws IOException {
        TextureForgeJson.write(paths.importDir().resolve("apply_report.json"), result);
        TextureForgeMarkdown.write(paths.importDir().resolve("apply_report.md"), markdown(result));
    }

    private static String markdown(TextureApplyResult result) {
        StringBuilder out = new StringBuilder(TextureForgeMarkdown.heading("TextureForge Apply Report"));
        out.append("- Dry run: ").append(result.dryRun()).append('\n');
        out.append("- Overwrite approved: ").append(result.overwriteApproved()).append('\n');
        out.append("- Mod ID filter: `").append(result.modidFilter().isBlank() ? "all" : result.modidFilter()).append("`\n");
        out.append("- Copied or would copy: ").append(result.copied()).append('\n');
        out.append("- Skipped: ").append(result.skipped()).append('\n');
        out.append("- Conflicts: ").append(result.conflicts()).append("\n\n");
        if (!result.backupFiles().isEmpty()) {
            out.append("## Backups\n\n");
            result.backupFiles().forEach(path -> out.append("- `").append(path).append("`\n"));
            out.append('\n');
        }
        out.append("## Actions\n\n");
        out.append("| Status | Asset | Staged | Target | Message |\n");
        out.append("| --- | --- | --- | --- | --- |\n");
        for (TextureApplyAction action : result.actions()) {
            out.append("| ").append(action.status())
                    .append(" | `").append(action.namespace()).append('/').append(action.relativePath()).append("`")
                    .append(" | `").append(action.stagedPath()).append("`")
                    .append(" | `").append(action.targetPath()).append("`")
                    .append(" | ").append(action.message().replace("|", "\\|")).append(" |\n");
        }
        return out.toString();
    }
}
