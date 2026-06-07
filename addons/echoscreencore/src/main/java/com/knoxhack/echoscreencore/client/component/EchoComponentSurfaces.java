package com.knoxhack.echoscreencore.client.component;

import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EchoComponentSurfaces {
    private EchoComponentSurfaces() {
    }

    public static boolean isGlass(EchoStyle style) {
        return style != null && "glass".equalsIgnoreCase(style.value("surface", ""));
    }

    public static boolean renderGlass(EchoRenderContext context, EchoStyle style, EchoRect bounds,
            int background, int border, int accent, boolean raised) {
        if (context == null || bounds == null || !isGlass(style)) {
            return false;
        }
        String depth = style.value("surface-depth", raised ? "raised" : "base")
                .strip()
                .toLowerCase(Locale.ROOT);
        int defaultShadow = switch (depth) {
            case "floating" -> 50;
            case "raised" -> 34;
            default -> 20;
        };
        int defaultGlow = switch (depth) {
            case "floating" -> 42;
            case "raised" -> raised ? 38 : 24;
            default -> raised ? 22 : 8;
        };
        int radius = EchoStyleValues.length(style, "border-radius", Math.min(bounds.width(), bounds.height()),
                context.theme().radius("md", 10), context.theme(), context.diagnostics());
        int shadow = EchoStyleValues.length(style, "shadow-strength", 100, defaultShadow,
                context.theme(), context.diagnostics());
        int glow = EchoStyleValues.length(style, "glow-strength", 100, defaultGlow,
                context.theme(), context.diagnostics());
        int textureAlpha = EchoStyleValues.length(style, "texture-alpha", 255, 255,
                context.theme(), context.diagnostics());
        int glowColor = EchoStyleValues.color(style, "accent-color", context.theme(), accent, context.diagnostics());
        boolean highlight = style.bool("inner-highlight", true);
        String cornerTreatment = style.value("corner-treatment", "bevel");
        List<EchoRenderBridge.GlassTextureLayer> textureLayers = textureLayers(style, context);
        if (context.accessibility().quietVisuals()) {
            shadow = Math.min(shadow, 14);
            glow = 0;
            textureAlpha = Math.min(textureAlpha, 90);
            textureLayers = quietTextureLayers(textureLayers);
        }
        context.render().glassPanel(context.graphics(), context.font(),
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                background, border, glowColor, radius, shadow, glow, highlight,
                cornerTreatment, textureLayers.isEmpty()
                        ? List.of(new EchoRenderBridge.GlassTextureLayer(
                                EchoStyleValues.texture(style, "background-texture", "", context.theme(), context.diagnostics()),
                                textureAlpha, 1))
                        : textureLayers,
                context.accessibility().quietVisuals());
        return true;
    }

    private static List<EchoRenderBridge.GlassTextureLayer> textureLayers(EchoStyle style, EchoRenderContext context) {
        ArrayList<EchoRenderBridge.GlassTextureLayer> layers = new ArrayList<>(3);
        addTextureLayer(layers, style, context, "", 255, 1);
        addTextureLayer(layers, style, context, "-2", 160, 1);
        addTextureLayer(layers, style, context, "-3", 120, 1);
        return List.copyOf(layers);
    }

    private static void addTextureLayer(List<EchoRenderBridge.GlassTextureLayer> layers, EchoStyle style,
            EchoRenderContext context, String suffix, int fallbackAlpha, int fallbackInset) {
        String texture = style.value("background-texture" + suffix, "");
        texture = EchoStyleValues.texture(texture, "", context.theme(), context.diagnostics());
        if (texture.isBlank()) {
            return;
        }
        int alpha = EchoStyleValues.length(style, "texture-alpha" + suffix, 255, fallbackAlpha,
                context.theme(), context.diagnostics());
        int inset = EchoStyleValues.length(style, "texture-inset" + suffix, 64, fallbackInset,
                context.theme(), context.diagnostics());
        EchoRenderBridge.TextureFit fit = EchoRenderBridge.TextureFit.from(style.value("texture-fit" + suffix, "stretch"));
        EchoRenderBridge.TextureRegion region = textureRegion(style.value("texture-region" + suffix, ""));
        EchoRenderBridge.GlassTextureLayer layer = new EchoRenderBridge.GlassTextureLayer(texture, alpha, inset, fit, region);
        if (layer.visible()) {
            layers.add(layer);
        }
    }

    private static EchoRenderBridge.TextureRegion textureRegion(String value) {
        if (value == null || value.isBlank()) {
            return EchoRenderBridge.TextureRegion.FULL;
        }
        String[] parts = value.strip().split("[,\\s]+");
        if (parts.length != 4) {
            return EchoRenderBridge.TextureRegion.FULL;
        }
        try {
            return new EchoRenderBridge.TextureRegion(
                    Float.parseFloat(parts[0]),
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3]));
        } catch (NumberFormatException exception) {
            return EchoRenderBridge.TextureRegion.FULL;
        }
    }

    private static List<EchoRenderBridge.GlassTextureLayer> quietTextureLayers(
            List<EchoRenderBridge.GlassTextureLayer> layers) {
        if (layers == null || layers.isEmpty()) {
            return List.of();
        }
        EchoRenderBridge.GlassTextureLayer first = layers.getFirst();
        return List.of(new EchoRenderBridge.GlassTextureLayer(first.texture(), Math.min(first.alpha(), 80),
                first.inset(), first.fit(), first.region()));
    }

    public static int glassFill(int color, int fallbackAlpha) {
        int alpha = (color >>> 24) & 255;
        if (alpha > 0) {
            return color;
        }
        return EchoRenderBridge.withAlpha(0xFF000000 | color, fallbackAlpha);
    }
}
