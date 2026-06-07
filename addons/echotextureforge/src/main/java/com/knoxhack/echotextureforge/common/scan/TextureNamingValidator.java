package com.knoxhack.echotextureforge.common.scan;

import com.knoxhack.echotextureforge.api.report.TextureAuditIssue;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextureNamingValidator {
    private static final Pattern LEGAL_PATH = Pattern.compile("[a-z0-9_./-]+");

    private TextureNamingValidator() {
    }

    public static List<TextureAuditIssue> validate(List<TextureSpec> specs, ResourceScanResult resources) {
        List<TextureAuditIssue> issues = new ArrayList<>();
        for (TextureSpec spec : specs) {
            validateSpec(spec, issues);
        }
        resources.namespaces().forEach((namespace, assets) ->
                assets.files().keySet().forEach(path -> validateResourcePath(namespace, path, issues)));
        return List.copyOf(issues);
    }

    private static void validateSpec(TextureSpec spec, List<TextureAuditIssue> issues) {
        String id = spec.assetId();
        if (!LEGAL_PATH.matcher(id).matches()) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "ILLEGAL_ASSET_ID",
                    spec.namespace(), id, spec.assetKind(), spec.outputPath(),
                    "Asset id contains characters outside lowercase letters, numbers, underscore, hyphen, dot, or slash."));
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("test_item") || lower.contains("temp_block") || lower.contains("new_texture")
                || lower.contains("placeholder") || lower.contains("todo") || lower.matches(".*random[_-]?[0-9a-f]{4,}.*")) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "PLACEHOLDER_NAME",
                    spec.namespace(), id, spec.assetKind(), spec.outputPath(),
                    "Asset name looks temporary or placeholder-like."));
        }
        String[] terms = lower.replace('/', '_').split("_");
        for (int i = 1; i < terms.length; i++) {
            if (!terms[i].isBlank() && terms[i].equals(terms[i - 1])) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.INFO, "DUPLICATE_NAME_TERM",
                        spec.namespace(), id, spec.assetKind(), spec.outputPath(),
                        "Asset name repeats the term '" + terms[i] + "'."));
                break;
            }
        }
        String textureLeaf = leafWithoutExtension(spec.outputPath());
        String idLeaf = leafWithoutExtension(id);
        if (!textureLeaf.isBlank() && !idLeaf.equals(textureLeaf)) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "TEXTURE_NAME_MISMATCH",
                    spec.namespace(), id, spec.assetKind(), spec.outputPath(),
                    "Texture filename '" + textureLeaf + "' does not match asset id leaf '" + idLeaf + "'."));
        }
        if (spec.isBlockLike() && !spec.blockstatePath().isBlank()) {
            String blockstateLeaf = leafWithoutExtension(spec.blockstatePath());
            if (!idLeaf.equals(blockstateLeaf)) {
                issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "BLOCKSTATE_NAME_MISMATCH",
                        spec.namespace(), id, spec.assetKind(), spec.blockstatePath(),
                        "Blockstate filename '" + blockstateLeaf + "' does not match block id leaf '" + idLeaf + "'."));
            }
        }
    }

    private static void validateResourcePath(String namespace, String path, List<TextureAuditIssue> issues) {
        if (!path.equals(path.toLowerCase(Locale.ROOT))) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "UPPERCASE_PATH",
                    namespace, leafWithoutExtension(path), null, path,
                    "Resource path contains uppercase characters."));
        }
        if (path.contains(" ")) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "SPACE_IN_PATH",
                    namespace, leafWithoutExtension(path), null, path,
                    "Resource path contains spaces."));
        }
        if (!LEGAL_PATH.matcher(path).matches()) {
            issues.add(new TextureAuditIssue(TextureAuditSeverity.WARNING, "ILLEGAL_RESOURCE_PATH",
                    namespace, leafWithoutExtension(path), null, path,
                    "Resource path contains unsupported characters."));
        }
    }

    private static String leafWithoutExtension(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String value = path.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }
}
