package com.knoxhack.echotextureforge.common.scan;

import com.knoxhack.echotextureforge.api.report.TextureAuditIssue;
import com.knoxhack.echotextureforge.api.report.TextureAuditSeverity;
import com.knoxhack.echotextureforge.api.scan.TextureValidationRules;
import com.knoxhack.echotextureforge.api.spec.TextureKind;
import com.knoxhack.echotextureforge.api.spec.TextureResolution;
import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public final class TextureImageValidator {
    private TextureImageValidator() {
    }

    public static List<TextureAuditIssue> validate(ResourceScanResult resources, List<TextureSpec> specs, TextureValidationRules rules) {
        Map<String, TextureSpec> specsByOutput = new HashMap<>();
        for (TextureSpec spec : specs) {
            specsByOutput.put(spec.namespace() + ":" + spec.outputPath(), spec);
        }

        List<TextureAuditIssue> issues = new ArrayList<>();
        resources.namespaces().forEach((namespace, assets) -> {
            for (String texturePath : assets.textureFiles()) {
                TextureSpec spec = specsByOutput.get(namespace + ":" + texturePath);
                TextureResolution expected = expectedResolution(texturePath, spec, rules);
                boolean requireTransparency = spec != null ? spec.transparencyRequired() : texturePath.startsWith("textures/item/");
                assets.firstPath(texturePath).ifPresent(path -> validateOne(namespace, texturePath, path, expected,
                        requireTransparency, rules, issues));
            }
        });
        return List.copyOf(issues);
    }

    private static TextureResolution expectedResolution(String texturePath, TextureSpec spec, TextureValidationRules rules) {
        if (spec != null) {
            return spec.expectedResolution();
        }
        if (rules.validate32x32() && (texturePath.startsWith("textures/item/")
                || texturePath.startsWith("textures/block/"))) {
            return rules.defaultResolution();
        }
        return null;
    }

    private static void validateOne(
            String namespace,
            String texturePath,
            Path physicalPath,
            TextureResolution expected,
            boolean requireTransparency,
            TextureValidationRules rules,
            List<TextureAuditIssue> issues) {
        BufferedImage image;
        try {
            image = ImageIO.read(physicalPath.toFile());
        } catch (IOException | RuntimeException exception) {
            issues.add(issue(TextureAuditSeverity.CRITICAL, "CORRUPTED_PNG", namespace, texturePath, physicalPath,
                    "PNG could not be read: " + exception.getMessage()));
            return;
        }
        if (image == null) {
            issues.add(issue(TextureAuditSeverity.CRITICAL, "CORRUPTED_PNG", namespace, texturePath, physicalPath,
                    "PNG could not be decoded by ImageIO."));
            return;
        }

        if (expected != null && (image.getWidth() != expected.width() || image.getHeight() != expected.height())) {
            issues.add(issue(TextureAuditSeverity.WARNING, "WRONG_TEXTURE_SIZE", namespace, texturePath, physicalPath,
                    "Expected " + expected.id() + " but found " + image.getWidth() + "x" + image.getHeight() + "."));
        }

        if (rules.requirePowerOfTwo() && (!powerOfTwo(image.getWidth()) || !powerOfTwo(image.getHeight()))) {
            issues.add(issue(TextureAuditSeverity.WARNING, "NOT_POWER_OF_TWO", namespace, texturePath, physicalPath,
                    "Texture dimensions should be power-of-two but found " + image.getWidth() + "x" + image.getHeight() + "."));
        }

        if (requireTransparency) {
            AlphaStats alpha;
            try {
                alpha = alphaStats(image);
            } catch (RuntimeException exception) {
                issues.add(issue(TextureAuditSeverity.CRITICAL, "CORRUPTED_PNG", namespace, texturePath, physicalPath,
                        "PNG pixel data could not be read: " + exception.getMessage()));
                return;
            }
            if (!alpha.hasTransparentPixel()) {
                issues.add(issue(TextureAuditSeverity.WARNING, "MISSING_TRANSPARENT_BACKGROUND", namespace, texturePath, physicalPath,
                        "Item-like texture has no transparent pixels; inventory sprites usually need transparent background."));
            } else if (alpha.transparentRatio() > 0.92D) {
                issues.add(issue(TextureAuditSeverity.INFO, "MOSTLY_EMPTY_CANVAS", namespace, texturePath, physicalPath,
                        "Texture canvas is more than 92% transparent; verify the sprite is not too small."));
            }
        }
    }

    private static TextureAuditIssue issue(TextureAuditSeverity severity, String code, String namespace,
                                           String texturePath, Path physicalPath, String message) {
        return new TextureAuditIssue(severity, code, namespace, idFromTexture(texturePath),
                kindFromTexture(texturePath), physicalPath.toString(), message);
    }

    private static String idFromTexture(String texturePath) {
        String path = texturePath;
        if (path.startsWith("textures/item/")) {
            path = path.substring("textures/item/".length());
        } else if (path.startsWith("textures/block/")) {
            path = path.substring("textures/block/".length());
        } else if (path.startsWith("textures/entity/")) {
            path = path.substring("textures/entity/".length());
        } else if (path.startsWith("textures/gui/")) {
            path = path.substring("textures/gui/".length());
        }
        return path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
    }

    private static TextureKind kindFromTexture(String texturePath) {
        if (texturePath.startsWith("textures/block/")) {
            return TextureKind.BLOCK;
        }
        if (texturePath.startsWith("textures/entity/")) {
            return TextureKind.ENTITY;
        }
        if (texturePath.startsWith("textures/gui/")) {
            return TextureKind.UI;
        }
        return TextureKind.ITEM;
    }

    private static boolean powerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    private static AlphaStats alphaStats(BufferedImage image) {
        int total = image.getWidth() * image.getHeight();
        int transparent = 0;
        boolean hasTransparent = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha < 250) {
                    transparent++;
                    hasTransparent = true;
                }
            }
        }
        return new AlphaStats(hasTransparent, total == 0 ? 0.0D : transparent / (double) total);
    }

    private record AlphaStats(boolean hasTransparentPixel, double transparentRatio) {
    }
}
