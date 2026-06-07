package com.knoxhack.echotextureforge.api.prompt;

import com.knoxhack.echotextureforge.api.spec.TextureSpec;
import com.knoxhack.echotextureforge.api.spec.TextureStyleFamily;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class TexturePromptTemplate {
    private TexturePromptTemplate() {
    }

    public static String singleTexturePrompt(TextureSpec spec) {
        TextureStyleFamily style = spec.styleFamily();
        List<String> mustHave = withDefaults(spec.mustHave(), List.of(
                "strong silhouette",
                "readable at Minecraft inventory size",
                "clear light/dark contrast",
                "clean pixel edges"));
        List<String> avoid = withDefaults(spec.avoid(), List.of(
                "no photorealistic rendering",
                "no blurry gradients",
                "no oversized glow",
                "no text",
                "no labels inside the texture",
                "no UI frame unless this is a UI texture",
                "no background scene",
                "no fake 3D render"));
        List<String> palette = new ArrayList<>(style.paletteHints());
        palette.addAll(spec.colorPaletteHints());
        if (!spec.palette().isBlank()) {
            palette.add(spec.palette());
        }

        return """
                Generate a Minecraft mod texture.

                Asset:
                - Mod ID: %s
                - Asset ID: %s
                - Asset Kind: %s
                - Texture Type: %s
                - Output Path: assets/%s/%s
                - Required Resolution: %s
                - Style Family: %s

                Visual Direction:
                %s
                Style family direction: %s
                Palette hints: %s
                Material hints: %s
                Lighting rules: %s
                Shape language: %s
                Silhouette notes: %s
                Minecraft readability notes: %s

                Hard Requirements:
                - %s pixels unless specified otherwise
                %s
                - Minecraft-style pixel art
                - clean silhouette
                - readable at inventory size
                - no photorealistic rendering
                - no blurry gradients
                - no text
                - no labels inside the texture
                - no UI frame unless this is a UI texture
                - no background scene
                - no fake 3D render
                - isolated asset only

                Must Have:
                %s

                Avoid:
                %s

                Return:
                - PNG texture only
                - transparent background where required
                - exact asset, not a mockup
                - one isolated texture only, no sheet
                """.formatted(
                spec.namespace(),
                spec.assetId(),
                spec.assetKind().id(),
                spec.textureType().id(),
                spec.namespace(),
                spec.outputPath(),
                spec.expectedResolution().id(),
                style.name(),
                visualDirection(spec),
                style.visualDirection(),
                String.join(", ", palette),
                String.join(", ", style.materialHints()),
                style.lightingRules(),
                style.shapeLanguage(),
                spec.silhouetteNotes().isBlank() ? "Use a distinct readable outline." : spec.silhouetteNotes(),
                spec.minecraftReadabilityNotes().isBlank()
                        ? "Texture must remain understandable at Minecraft inventory and block-view scale."
                        : spec.minecraftReadabilityNotes(),
                spec.expectedResolution().id(),
                spec.transparencyRequired()
                        ? "- transparent background for item/UI icon use"
                        : "- block-ready surface; use transparency only if the texture type requires cutout pixels",
                bulletList(mustHave),
                bulletList(mergedAvoid(avoid, style.avoidList()))).stripTrailing() + "\n";
    }

    private static String visualDirection(TextureSpec spec) {
        if (!spec.generatedPrompt().isBlank()) {
            return spec.generatedPrompt();
        }
        if (!spec.notes().isBlank()) {
            return spec.notes();
        }
        return spec.styleFamily().exampleDirection();
    }

    private static List<String> withDefaults(List<String> values, List<String> defaults) {
        return values == null || values.isEmpty() ? defaults : values;
    }

    private static List<String> mergedAvoid(List<String> base, List<String> styleAvoid) {
        List<String> merged = new ArrayList<>(base);
        for (String avoid : styleAvoid) {
            if (!merged.contains(avoid)) {
                merged.add(avoid);
            }
        }
        return merged;
    }

    private static String bulletList(List<String> values) {
        StringJoiner joiner = new StringJoiner("\n");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add("- " + value.strip());
            }
        }
        String result = joiner.toString();
        return result.isBlank() ? "- follow the asset spec exactly" : result;
    }
}
