package com.knoxhack.echoscreencore.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import java.util.ArrayDeque;
import java.util.Deque;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

public final class EchoRenderBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Method renderCoreFrame;
    private Object quietOptions;
    private Object cyberglassOptions;
    private Class<?> hostClass;
    private boolean renderCoreChecked;
    private final Deque<Boolean> scissorStack = new ArrayDeque<>();
    private final Deque<EchoRect> logicalClipStack = new ArrayDeque<>();
    private final Map<String, Optional<Identifier>> textureCache = new HashMap<>();
    private final Map<TextureAlphaKey, Optional<Identifier>> alphaTextureCache = new HashMap<>();

    public void beginFrame() {
        scissorStack.clear();
        logicalClipStack.clear();
    }

    public void endFrame(GuiGraphicsExtractor graphics) {
        while (!scissorStack.isEmpty()) {
            if (scissorStack.pop() && graphics != null) {
                try {
                    graphics.disableScissor();
                } catch (IllegalStateException exception) {
                    LOGGER.warn("ScreenCore scissor stack was already empty while ending a frame.", exception);
                    scissorStack.clear();
                    return;
                }
            }
            if (!logicalClipStack.isEmpty()) {
                logicalClipStack.pop();
            }
        }
        logicalClipStack.clear();
    }

    public void fill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        if (graphics != null && width > 0 && height > 0) {
            graphics.fill(x, y, x + width, y + height, color);
        }
    }

    public void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        if (graphics != null && width > 0 && height > 0) {
            graphics.outline(x, y, width, height, color);
        }
    }

    public void panel(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            int background, int border, boolean quiet) {
        fill(graphics, x, y, width, height, background);
        if (!quiet && drawRenderCoreFrame(graphics, font, x, y, width, height)) {
            return;
        }
        outline(graphics, x, y, width, height, border);
        if (width > 18 && height > 8) {
            fill(graphics, x + 1, y + 1, Math.max(8, width / 4), 1, withAlpha(border, 190));
            fill(graphics, x + 1, y + height - 2, Math.max(8, width / 5), 1, withAlpha(border, 150));
        }
    }

    public void glassPanel(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            int background, int border, int accent, int cornerSize, int shadowStrength, int glowStrength,
            boolean innerHighlight, String cornerTreatment, String texture, int textureAlpha, boolean quiet) {
        glassPanel(graphics, font, x, y, width, height, background, border, accent, cornerSize, shadowStrength,
                glowStrength, innerHighlight, cornerTreatment,
                List.of(new GlassTextureLayer(texture, textureAlpha, 1)), quiet);
    }

    public void glassPanel(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            int background, int border, int accent, int cornerSize, int shadowStrength, int glowStrength,
            boolean innerHighlight, String cornerTreatment, List<GlassTextureLayer> textureLayers, boolean quiet) {
        if (graphics == null || width <= 0 || height <= 0) {
            return;
        }
        boolean faceted = !"rounded".equalsIgnoreCase(cornerTreatment == null ? "" : cornerTreatment.strip());
        int corner = Math.max(0, Math.min(Math.min(width, height) / 2, cornerSize));
        if (!quiet && shadowStrength > 0) {
            int shadow = Math.max(0, Math.min(120, shadowStrength));
            surfaceFill(graphics, x + 2, y + 3, width, height, corner, faceted, withAlpha(0xFF000000, shadow));
            if (height > 30 && width > 30) {
                surfaceFill(graphics, x + 4, y + 7, width - 2, height - 2, Math.max(0, corner - 1), faceted,
                        withAlpha(0xFF000000, Math.max(0, shadow / 2)));
            }
        }
        if (!quiet && glowStrength > 0) {
            int glow = Math.max(0, Math.min(100, glowStrength));
            surfaceFill(graphics, x - 2, y - 1, width + 4, height + 3, corner + 2, faceted, withAlpha(accent, glow));
        }
        int strokeAlpha = Math.max(0, Math.min(180, (border >>> 24) & 255));
        if (strokeAlpha > 0) {
            surfaceFill(graphics, x, y, width, height, corner, faceted, border);
            surfaceFill(graphics, x + 1, y + 1, Math.max(0, width - 2), Math.max(0, height - 2),
                    Math.max(0, corner - 1), faceted, background);
        } else {
            surfaceFill(graphics, x, y, width, height, corner, faceted, background);
        }
        drawTextureLayers(graphics, x, y, width, height, textureLayers);
        if (innerHighlight && width > 14 && height > 10) {
            int inset = Math.max(3, Math.min(10, corner / 2 + 2));
            fill(graphics, x + inset, y + 2, Math.max(6, width - inset * 2), 1, withAlpha(0xFFFFFFFF, 52));
            fill(graphics, x + inset + 2, y + 4, Math.max(4, width / 3), 1, withAlpha(0xFFFFFFFF, 28));
            fill(graphics, x + 2, y + inset, 1, Math.max(8, Math.min(height - inset * 2, height / 2)),
                    withAlpha(0xFFFFFFFF, 20));
            fill(graphics, x + width - Math.max(inset + 2, width / 5), y + height - 3,
                    Math.max(6, Math.min(width / 4, width - inset * 2)), 1, withAlpha(accent, 48));
            if (faceted && corner > 2) {
                fill(graphics, x + 1, y + corner, corner, 1, withAlpha(0xFFFFFFFF, 36));
                fill(graphics, x + width - corner - 1, y + height - corner - 1, corner, 1, withAlpha(accent, 44));
            }
        }
        if (!quiet && width >= 120 && height >= 44 && glowStrength >= 12) {
            drawRenderCoreCyberglassFrame(graphics, font, x, y, width, height);
        }
    }

    private void drawTextureLayers(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            List<GlassTextureLayer> layers) {
        if (layers == null || layers.isEmpty()) {
            return;
        }
        for (GlassTextureLayer layer : layers) {
            if (layer == null || !layer.visible()) {
                continue;
            }
            texture(layer.texture()).ifPresent(base -> {
                Optional<Identifier> resolved = alphaTexture(base, layer.alpha());
                Identifier id = resolved.orElse(base);
                int inset = Math.max(0, Math.min(Math.min(width, height) / 2 - 1, layer.inset()));
                int drawX = x + inset;
                int drawY = y + inset;
                int drawW = Math.max(0, width - inset * 2);
                int drawH = Math.max(0, height - inset * 2);
                if (drawW <= 0 || drawH <= 0) {
                    return;
                }
                enableScissor(graphics, drawX, drawY, drawW, drawH);
                try {
                    drawTextureLayer(graphics, id, drawX, drawY, drawW, drawH, layer);
                } finally {
                    disableScissor(graphics);
                }
            });
        }
    }

    private static void drawTextureLayer(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
            int width, int height, GlassTextureLayer layer) {
        TextureRegion region = layer == null ? TextureRegion.FULL : layer.region();
        if (layer != null && layer.fit() == TextureFit.NINE_SLICE) {
            blitNineSlice(graphics, texture, x, y, width, height, region);
            return;
        }
        graphics.blit(texture, x, y, x + width, y + height, region.u0(), region.u1(), region.v0(), region.v1());
    }

    private static void blitNineSlice(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
            int width, int height, TextureRegion region) {
        if (graphics == null || texture == null || width <= 0 || height <= 0) {
            return;
        }
        int left = Math.min(Math.max(1, width / 2), Math.max(4, Math.min(10, width / 5)));
        int right = Math.min(left, Math.max(0, width - left));
        int top = Math.min(Math.max(1, height / 2), Math.max(3, Math.min(8, height / 3)));
        int bottom = Math.min(top, Math.max(0, height - top));
        float regionW = Math.max(0.001F, region.u1() - region.u0());
        float regionH = Math.max(0.001F, region.v1() - region.v0());
        float uSlice = Math.min(regionW * 0.5F, regionW * 0.18F);
        float vSlice = Math.min(regionH * 0.5F, regionH * 0.30F);
        int[] dx = {x, x + left, x + Math.max(left, width - right), x + width};
        int[] dy = {y, y + top, y + Math.max(top, height - bottom), y + height};
        float[] u = {region.u0(), region.u0() + uSlice, region.u1() - uSlice, region.u1()};
        float[] v = {region.v0(), region.v0() + vSlice, region.v1() - vSlice, region.v1()};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dx[col + 1] <= dx[col] || dy[row + 1] <= dy[row]
                        || u[col + 1] <= u[col] || v[row + 1] <= v[row]) {
                    continue;
                }
                graphics.blit(texture, dx[col], dy[row], dx[col + 1], dy[row + 1],
                        u[col], u[col + 1], v[row], v[row + 1]);
            }
        }
    }

    public void enableScissor(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        EchoRect clip = width > 0 && height > 0 ? intersect(currentClip(), new EchoRect(x, y, width, height)) : EchoRect.ZERO;
        logicalClipStack.push(clip);
        if (graphics != null && clip.width() > 0 && clip.height() > 0) {
            graphics.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
            scissorStack.push(Boolean.TRUE);
        } else {
            scissorStack.push(Boolean.FALSE);
        }
    }

    public void disableScissor(GuiGraphicsExtractor graphics) {
        if (scissorStack.isEmpty()) {
            LOGGER.warn("ScreenCore ignored an unmatched scissor pop.");
            return;
        }
        if (scissorStack.pop() && graphics != null) {
            try {
                graphics.disableScissor();
            } catch (IllegalStateException exception) {
                LOGGER.warn("ScreenCore ignored a Minecraft scissor stack underflow.", exception);
                scissorStack.clear();
                logicalClipStack.clear();
            }
        }
        if (!logicalClipStack.isEmpty()) {
            logicalClipStack.pop();
        }
    }

    public EchoRect currentClip() {
        return logicalClipStack.peek();
    }

    private static EchoRect intersect(EchoRect first, EchoRect second) {
        if (first == null) {
            return second == null ? EchoRect.ZERO : second;
        }
        if (second == null) {
            return first;
        }
        int left = Math.max(first.x(), second.x());
        int top = Math.max(first.y(), second.y());
        int right = Math.min(first.right(), second.right());
        int bottom = Math.min(first.bottom(), second.bottom());
        return new EchoRect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private void surfaceFill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int corner,
            boolean faceted, int color) {
        if (faceted) {
            facetedFill(graphics, x, y, width, height, corner, color);
        } else {
            roundedFill(graphics, x, y, width, height, corner, color);
        }
    }

    private void facetedFill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int corner, int color) {
        if (graphics == null || width <= 0 || height <= 0 || ((color >>> 24) & 255) <= 0) {
            return;
        }
        int c = Math.max(0, Math.min(Math.min(width, height) / 3, corner));
        if (c <= 1) {
            fill(graphics, x, y, width, height, color);
            return;
        }
        int middleY = y + c;
        int middleH = height - c * 2;
        if (middleH > 0) {
            fill(graphics, x, middleY, width, middleH, color);
        }
        for (int dy = 0; dy < c; dy++) {
            int inset = c - dy;
            int rowW = Math.max(0, width - inset * 2);
            fill(graphics, x + inset, y + dy, rowW, 1, color);
            fill(graphics, x + inset, y + height - dy - 1, rowW, 1, color);
        }
    }

    private void roundedFill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
        if (graphics == null || width <= 0 || height <= 0 || ((color >>> 24) & 255) <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(Math.min(width, height) / 2, radius));
        if (r <= 1) {
            fill(graphics, x, y, width, height, color);
            return;
        }
        int middleTop = y + r;
        int middleH = height - r * 2;
        if (middleH > 0) {
            fill(graphics, x, middleTop, width, middleH, color);
        }
        for (int dy = 0; dy < r; dy++) {
            double yy = r - dy - 0.5D;
            int inset = Math.max(0, (int) Math.floor(r - Math.sqrt(Math.max(0.0D, r * r - yy * yy))));
            int rowW = Math.max(0, width - inset * 2);
            fill(graphics, x + inset, y + dy, rowW, 1, color);
            fill(graphics, x + inset, y + height - dy - 1, rowW, 1, color);
        }
    }

    private Optional<Identifier> texture(String raw) {
        String value = raw == null ? "" : raw.strip();
        if (value.isBlank()) {
            return Optional.empty();
        }
        return textureCache.computeIfAbsent(value, key -> {
            try {
                Identifier id = Identifier.parse(key);
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft == null || minecraft.getResourceManager().getResource(id).isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(id);
            } catch (LinkageError | RuntimeException exception) {
                return Optional.empty();
            }
        });
    }

    private Optional<Identifier> alphaTexture(Identifier source, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        if (source == null || clamped <= 0) {
            return Optional.empty();
        }
        if (clamped >= 255) {
            return Optional.of(source);
        }
        return alphaTextureCache.computeIfAbsent(new TextureAlphaKey(source, clamped), this::createAlphaTexture);
    }

    private Optional<Identifier> createAlphaTexture(TextureAlphaKey key) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(key.source());
        if (resource.isEmpty()) {
            return Optional.of(key.source());
        }
        Identifier generated = generatedAlphaId(key.source(), key.alpha());
        try (InputStream input = resource.get().open(); NativeImage source = NativeImage.read(input)) {
            NativeImage adjusted = source.mappedCopy(pixel -> multiplyAlpha(pixel, key.alpha()));
            DynamicTexture texture = new DynamicTexture(
                    () -> "ScreenCore glass alpha " + key.source() + " @" + key.alpha(), adjusted);
            minecraft.getTextureManager().register(generated, texture);
            return Optional.of(generated);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Falling back to baked alpha for ScreenCore glass texture {} at alpha {}", key.source(),
                    key.alpha(), exception);
            return Optional.of(key.source());
        }
    }

    private static Identifier generatedAlphaId(Identifier source, int alpha) {
        String safePath = source.getPath().replace('/', '_').replace('.', '_');
        return Identifier.fromNamespaceAndPath("echoscreencore",
                "generated/glass_alpha/" + source.getNamespace() + "/" + safePath + "_" + alpha);
    }

    private static int multiplyAlpha(int argb, int alpha) {
        int sourceAlpha = (argb >>> 24) & 255;
        int adjustedAlpha = Math.round(sourceAlpha * (Math.max(0, Math.min(255, alpha)) / 255.0F));
        return (argb & 0x00FFFFFF) | (adjustedAlpha << 24);
    }

    private boolean drawRenderCoreFrame(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) {
            return false;
        }
        try {
            ensureRenderCore();
            if (renderCoreFrame == null || quietOptions == null) {
                return false;
            }
            renderCoreFrame.invoke(null, graphics, font, null, x, y, width, height, quietOptions);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            renderCoreFrame = null;
            quietOptions = null;
            hostClass = null;
            return false;
        }
    }

    private boolean drawRenderCoreCyberglassFrame(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) {
            return false;
        }
        try {
            ensureRenderCore();
            if (renderCoreFrame == null || cyberglassOptions == null) {
                return false;
            }
            renderCoreFrame.invoke(null, graphics, font, null, x, y, width, height, cyberglassOptions);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            renderCoreFrame = null;
            quietOptions = null;
            cyberglassOptions = null;
            hostClass = null;
            return false;
        }
    }

    private void ensureRenderCore() throws ReflectiveOperationException {
        if (renderCoreChecked) {
            return;
        }
        renderCoreChecked = true;
        Class<?> visuals = Class.forName("com.knoxhack.echorendercore.client.RenderCoreScreenVisuals");
        hostClass = Class.forName("com.knoxhack.echorendercore.client.RenderCoreScreenVisualHost");
        Class<?> optionsClass = Class.forName("com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions");
        quietOptions = optionsClass.getMethod("quiet").invoke(null);
        Object builder = optionsClass.getMethod("cyberglass", String.class).invoke(null, "");
        Class<?> builderClass = builder.getClass();
        builderClass.getMethod("backdrop", boolean.class).invoke(builder, false);
        builderClass.getMethod("scanlines", boolean.class).invoke(builder, false);
        builderClass.getMethod("glassGlints", boolean.class).invoke(builder, true);
        builderClass.getMethod("cornerBrackets", boolean.class).invoke(builder, true);
        builderClass.getMethod("accentRails", boolean.class).invoke(builder, true);
        builderClass.getMethod("edgeGlow", boolean.class).invoke(builder, true);
        builderClass.getMethod("chromaticEdge", boolean.class).invoke(builder, true);
        builderClass.getMethod("quietFallback", boolean.class).invoke(builder, false);
        cyberglassOptions = builderClass.getMethod("build").invoke(builder);
        renderCoreFrame = visuals.getMethod("drawFrame", GuiGraphicsExtractor.class, Font.class, hostClass,
            int.class, int.class, int.class, int.class, optionsClass);
    }

    public enum TextureFit {
        STRETCH,
        NINE_SLICE;

        public static TextureFit from(String value) {
            String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
            return switch (normalized) {
                case "nine-slice", "nine_slice", "sliced", "slice" -> NINE_SLICE;
                default -> STRETCH;
            };
        }
    }

    public record TextureRegion(float u0, float v0, float u1, float v1) {
        public static final TextureRegion FULL = new TextureRegion(0.0F, 0.0F, 1.0F, 1.0F);

        public TextureRegion {
            u0 = clampUnit(u0);
            v0 = clampUnit(v0);
            u1 = clampUnit(u1);
            v1 = clampUnit(v1);
            if (u1 <= u0) {
                u0 = 0.0F;
                u1 = 1.0F;
            }
            if (v1 <= v0) {
                v0 = 0.0F;
                v1 = 1.0F;
            }
        }

        private static float clampUnit(float value) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return 0.0F;
            }
            return Math.max(0.0F, Math.min(1.0F, value));
        }
    }

    public record GlassTextureLayer(String texture, int alpha, int inset, TextureFit fit, TextureRegion region) {
        public GlassTextureLayer(String texture, int alpha, int inset) {
            this(texture, alpha, inset, TextureFit.STRETCH, TextureRegion.FULL);
        }

        public GlassTextureLayer {
            texture = texture == null ? "" : texture.strip();
            alpha = Math.max(0, Math.min(255, alpha));
            inset = Math.max(0, inset);
            fit = fit == null ? TextureFit.STRETCH : fit;
            region = region == null ? TextureRegion.FULL : region;
        }

        public boolean visible() {
            return !texture.isBlank() && alpha > 0;
        }
    }

    private record TextureAlphaKey(Identifier source, int alpha) {
    }
}
