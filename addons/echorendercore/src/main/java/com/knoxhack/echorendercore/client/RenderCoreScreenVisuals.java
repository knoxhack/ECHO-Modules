package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualEffectProfile;
import com.knoxhack.echorendercore.profile.VisualProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class RenderCoreScreenVisuals {
   private RenderCoreScreenVisuals() {
   }

   public static ScreenVisualData resolve(RenderCoreScreenVisualHost host) {
      if (host == null || host.screenVisualProfileId() == null) {
         return ScreenVisualData.EMPTY;
      }
      Identifier profileId = host.screenVisualProfileId();
      VisualProfile profile = RenderCoreProfiles.visual(profileId);
      float partialTick = 0.0F;
      Minecraft minecraft = Minecraft.getInstance();
      float age = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + partialTick;
      VisualContext context = new VisualContext(
         profileId,
         host.screenVisualState(),
         host.screenVisualVariant(),
         host.screenVisualProgress(),
         age,
         partialTick,
         false,
         false,
         0
      );
      VisualEffectProfile effect = profile == null ? VisualEffectProfile.NONE : profile.effect();
      int accent = RenderCoreEffectPipeline.color(0xFF66E8FF, effect);
      int panel = RenderCoreEffectPipeline.color(0xEE061018, effect);
      int border = RenderCoreEffectPipeline.color(0xAA38DFF4, effect);
      ThemeCoreColors themeCore = ThemeCoreColors.resolve();
      if (themeCore != null) {
         accent = RenderCoreEffectPipeline.color(themeCore.accent(), effect);
         panel = RenderCoreEffectPipeline.color(themeCore.panel(), effect);
         border = RenderCoreEffectPipeline.color(themeCore.border(), effect);
      }
      return new ScreenVisualData(profileId, profile, context, effect, accent, panel, border, host.screenSurfaceType());
   }

   public static void drawFrame(GuiGraphicsExtractor graphics, Font font, RenderCoreScreenVisualHost host,
         int x, int y, int width, int height, String label) {
      drawFrame(graphics, font, host, x, y, width, height, RenderCoreScreenFrameOptions.legacy(label));
   }

   public static void drawFrame(GuiGraphicsExtractor graphics, Font font, RenderCoreScreenVisualHost host,
         int x, int y, int width, int height, RenderCoreScreenFrameOptions options) {
      if (graphics == null || width <= 0 || height <= 0) {
         return;
      }
      RenderCoreScreenFrameOptions resolvedOptions = options == null ? RenderCoreScreenFrameOptions.legacy("") : options;
      ScreenVisualData data = resolve(host);
      if (data == ScreenVisualData.EMPTY && !resolvedOptions.quietFallback()) {
         return;
      }
      int accent = data == ScreenVisualData.EMPTY ? 0xFF38E8FF : data.accentColor();
      int border = data == ScreenVisualData.EMPTY ? 0x9938E8FF : data.borderColor();
      int panel = data == ScreenVisualData.EMPTY ? 0xEE061018 : data.panelColor();
      int secondary = secondaryColor(data, accent);
      if (resolvedOptions.style() == RenderCoreScreenChromeStyle.MINIMAL) {
         drawMinimalFrame(graphics, x, y, width, height, border, accent, resolvedOptions);
      } else {
         drawCyberglassFrame(graphics, font, x, y, width, height, panel, border, accent, secondary, data,
            resolvedOptions);
      }
   }

   private static void drawMinimalFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
         int border, int accent, RenderCoreScreenFrameOptions options) {
      graphics.outline(x, y, width, height, withAlpha(border, options.quietFallback() ? 0x72 : 0x99));
      if (options.accentRails() && width >= 48 && height >= 8) {
         graphics.fill(x, y, x + Math.max(24, width / 7), y + 1, withAlpha(accent, 0xAA));
         graphics.fill(x, y + height - 1, x + Math.max(18, width / 9), y + height, withAlpha(accent, 0x77));
      }
   }

   private static void drawCyberglassFrame(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
         int height, int panel, int border, int accent, int secondary, ScreenVisualData data,
         RenderCoreScreenFrameOptions options) {
      int glowAlpha = switch (options.style()) {
         case NEON -> 0x5F;
         case TERMINAL -> 0x42;
         case HOLOGRAM -> 0x38;
         default -> 0x34;
      };
      int glassAlpha = switch (options.style()) {
         case NEON -> 0x52;
         case TERMINAL -> 0x64;
         case HOLOGRAM -> 0x42;
         default -> 0x58;
      };

      if (options.backdrop() && width > 4 && height > 4) {
         graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, withAlpha(panel, glassAlpha));
         int shadowH = Math.max(2, Math.min(14, height / 6));
         graphics.fill(x + 2, y + height - shadowH - 1, x + width - 2, y + height - 2, 0x24000000);
         graphics.fill(x + 2, y + 2, x + width - 2, y + Math.min(y + height - 2, y + 8), withAlpha(accent, 0x10));
      }

      if (options.drawScanlines() && data != ScreenVisualData.EMPTY && width > 8 && height > 10) {
         drawScanlines(graphics, x, y, width, height, accent, data, options);
      }

      if (options.edgeGlow() && width > 6 && height > 6) {
         int animatedGlow = animatedAlpha(glowAlpha, data, 0.22F);
         graphics.outline(x - 1, y - 1, width + 2, height + 2, withAlpha(accent, animatedGlow));
         if (width > 24 && height > 24) {
            graphics.outline(x - 2, y - 2, width + 4, height + 4, withAlpha(accent, animatedGlow / 2));
         }
      }

      if (options.chromaticEdge() && width > 8 && height > 8) {
         graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, withAlpha(secondary, 0x66));
         graphics.fill(x + width - 1, y + 2, x + width, y + height - 2, withAlpha(secondary, 0x48));
         graphics.fill(x + 3, y + 1, x + Math.max(4, width / 4), y + 2, withAlpha(secondary, 0x38));
      }

      graphics.outline(x, y, width, height, withAlpha(border, 0xC8));
      if (width > 7 && height > 7) {
         graphics.outline(x + 2, y + 2, width - 4, height - 4, withAlpha(accent, 0x45));
      }
      if (options.backdrop() && width > 11 && height > 11) {
         graphics.outline(x + 4, y + 4, width - 8, height - 8, 0x18FFFFFF);
      }

      if (options.accentRails() && width >= 40 && height >= 8) {
         drawAccentRails(graphics, x, y, width, height, accent, secondary, options);
      }
      if (options.cornerBrackets() && width >= 28 && height >= 18) {
         drawCornerBrackets(graphics, x, y, width, height, accent, secondary, options);
      }
      if (options.glassGlints() && width >= 70 && height >= 24) {
         drawGlassGlints(graphics, x, y, width, height);
      }
      drawLabel(graphics, font, x, y, width, height, accent, secondary, options);
   }

   private static void drawAccentRails(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
         int accent, int secondary, RenderCoreScreenFrameOptions options) {
      int topRail = Math.max(32, Math.min(width - 16, width / 3));
      int bottomRail = Math.max(28, Math.min(width - 18, width / 4));
      int railAlpha = options.style() == RenderCoreScreenChromeStyle.NEON ? 0xE4 : 0xB8;
      graphics.fill(x + 8, y + 2, x + topRail, y + 3, withAlpha(accent, railAlpha));
      graphics.fill(x + Math.max(12, width / 2), y + 2, x + width - 8, y + 3, withAlpha(secondary, railAlpha / 2));
      graphics.fill(x + width - bottomRail, y + height - 3, x + width - 8, y + height - 2,
         withAlpha(secondary, railAlpha));
      graphics.fill(x + 8, y + height - 3, x + Math.max(9, width / 5), y + height - 2,
         withAlpha(accent, railAlpha / 2));
   }

   private static void drawCornerBrackets(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
         int accent, int secondary, RenderCoreScreenFrameOptions options) {
      int tick = Math.max(14, Math.min(52, Math.min(width, height) / 5));
      int shortTick = Math.max(8, tick / 2);
      int alpha = options.style() == RenderCoreScreenChromeStyle.NEON ? 0xFF : 0xD6;
      int primary = withAlpha(accent, alpha);
      int chroma = withAlpha(secondary, Math.max(0x44, alpha / 2));

      graphics.fill(x, y, x + tick, y + 1, primary);
      graphics.fill(x, y, x + 1, y + tick, primary);
      graphics.fill(x + 2, y + 2, x + shortTick, y + 3, chroma);

      graphics.fill(x + width - tick, y, x + width, y + 1, primary);
      graphics.fill(x + width - 1, y, x + width, y + tick, primary);
      graphics.fill(x + width - shortTick, y + 2, x + width - 2, y + 3, chroma);

      graphics.fill(x, y + height - 1, x + tick, y + height, primary);
      graphics.fill(x, y + height - tick, x + 1, y + height, primary);
      graphics.fill(x + 2, y + height - 3, x + shortTick, y + height - 2, chroma);

      graphics.fill(x + width - tick, y + height - 1, x + width, y + height, primary);
      graphics.fill(x + width - 1, y + height - tick, x + width, y + height, primary);
      graphics.fill(x + width - shortTick, y + height - 3, x + width - 2, y + height - 2, chroma);
   }

   private static void drawScanlines(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent,
         ScreenVisualData data, RenderCoreScreenFrameOptions options) {
      float strength = data.effect().scanlineStrength();
      if (options.style() == RenderCoreScreenChromeStyle.TERMINAL && strength < 0.18F) {
         strength = 0.18F;
      } else if (strength < 0.055F) {
         strength = 0.055F;
      }
      float minStrength = options.scanlinesBehindContent() ? 0.025F : 0.055F;
      float maxStrength = options.scanlinesBehindContent() ? 0.12F : 0.22F;
      int alpha = Math.round(Math.min(maxStrength, Math.max(minStrength, strength)) * 255.0F);
      int scanColor = withAlpha(accent, alpha);
      int step = options.style() == RenderCoreScreenChromeStyle.TERMINAL ? 6 : 8;
      for (int line = y + 5; line < y + height - 4; line += step) {
         graphics.fill(x + 3, line, x + width - 3, line + 1, scanColor);
      }
   }

   private static void drawGlassGlints(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
      int glint = 0x42FFFFFF;
      graphics.fill(x + 12, y + 5, x + Math.max(24, width / 5), y + 6, glint);
      graphics.fill(x + 16, y + 8, x + Math.max(22, width / 8), y + 9, 0x24FFFFFF);
      graphics.fill(x + width - Math.max(36, width / 8), y + 5, x + width - 14, y + 6, 0x2EFFFFFF);
      if (height > 58) {
         graphics.fill(x + 5, y + height / 2, x + 6, y + Math.min(height - 10, height / 2 + 18), 0x1EFFFFFF);
      }
   }

   private static void drawLabel(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
         int accent, int secondary, RenderCoreScreenFrameOptions options) {
      String label = options.label();
      if (!options.drawLabel() || font == null || label.isBlank() || width < 80 || height < 20) {
         return;
      }
      String trimmed = font.plainSubstrByWidth(label, width - 28);
      int labelW = Math.min(width - 16, font.width(trimmed) + 18);
      graphics.fill(x + 8, y + 5, x + 8 + labelW, y + 18, 0x66040A12);
      graphics.fill(x + 8, y + 5, x + 8 + labelW, y + 6, withAlpha(secondary, 0x55));
      graphics.outline(x + 8, y + 5, labelW, 13, withAlpha(accent, 0x70));
      graphics.text(font, trimmed, x + 14, y + 8, withAlpha(accent, 0xF0), false);
   }

   private static int secondaryColor(ScreenVisualData data, int accent) {
      int fallback = blendRgb(accent, 0xFFFF45F6, 0.45F);
      if (data == ScreenVisualData.EMPTY || data.effect().bloomTint() == null) {
         return fallback;
      }
      return RenderCoreEffectPipeline.color(data.effect().effectiveBloomTint(fallback), data.effect());
   }

   private static int animatedAlpha(int baseAlpha, ScreenVisualData data, float amount) {
      if (data == ScreenVisualData.EMPTY || data.effect().pulseSpeed() <= 0.0F) {
         return baseAlpha;
      }
      float seconds = (System.currentTimeMillis() % 3_600_000L) / 1000.0F;
      float pulse = (float)((Math.sin(seconds * data.effect().pulseSpeed() * Math.PI * 2.0D) + 1.0D) * 0.5D);
      return clamp(Math.round(baseAlpha * (1.0F - amount + pulse * amount)));
   }

   private static int withAlpha(int color, int alpha) {
      return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
   }

   private static int blendRgb(int left, int right, float rightWeight) {
      float amount = Math.max(0.0F, Math.min(1.0F, rightWeight));
      int lr = (left >>> 16) & 0xFF;
      int lg = (left >>> 8) & 0xFF;
      int lb = left & 0xFF;
      int rr = (right >>> 16) & 0xFF;
      int rg = (right >>> 8) & 0xFF;
      int rb = right & 0xFF;
      int r = Math.round(lr + (rr - lr) * amount);
      int g = Math.round(lg + (rg - lg) * amount);
      int b = Math.round(lb + (rb - lb) * amount);
      return 0xFF000000 | (r << 16) | (g << 8) | b;
   }

   private static int clamp(int value) {
      return Math.max(0, Math.min(255, value));
   }

   public record ScreenVisualData(
      Identifier profileId,
      VisualProfile profile,
      VisualContext context,
      VisualEffectProfile effect,
      int accentColor,
      int panelColor,
      int borderColor,
      String surfaceType
   ) {
      private static final ScreenVisualData EMPTY = new ScreenVisualData(
         null,
         null,
         null,
         VisualEffectProfile.NONE,
         0xFF66E8FF,
         0xEE061018,
         0xAA38DFF4,
         "screen"
      );
   }

   private record ThemeCoreColors(int accent, int panel, int border) {
      private static ThemeCoreColors resolve() {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player == null) {
            return null;
         }
         try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Object colors = api.getMethod("getColors", net.minecraft.world.entity.player.Player.class).invoke(null, minecraft.player);
            int accent = ((Integer) colors.getClass().getMethod("primary").invoke(colors)).intValue();
            int panel = ((Integer) colors.getClass().getMethod("panel").invoke(colors)).intValue();
            int border = ((Integer) colors.getClass().getMethod("border").invoke(colors)).intValue();
            return new ThemeCoreColors(accent, panel, border);
         } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
         }
      }
   }
}
