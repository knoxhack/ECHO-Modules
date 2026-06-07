package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.particle.RenderCoreSoftParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class RenderCoreSoftParticle extends SingleQuadParticle {
   private final SpriteSet sprites;
   private final float startAlpha;
   private final boolean fade;
   private final boolean fullbright;
   private final String style;
   private final float spin;
   private final float stretch;
   private final float flicker;
   private final float flickerPhase;
   private final boolean grows;

   private RenderCoreSoftParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
         double zSpeed, SpriteSet sprites, RenderCoreSoftParticleOptions options, boolean wisp) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.first());
      this.sprites = sprites;
      this.fade = options.fade();
      this.fullbright = options.fullbright();
      this.style = resolvedStyle(options.style(), wisp);
      this.spin = options.spin();
      this.stretch = options.stretch();
      this.flicker = options.flicker();
      this.flickerPhase = this.random.nextFloat() * Mth.TWO_PI;
      this.grows = wisp || this.style.endsWith("_ribbon") || this.style.endsWith("_stream");
      this.hasPhysics = false;
      this.friction = options.drag();
      this.lifetime = Math.max(1, options.lifetime());
      float baseSize = wisp ? 0.32F : 0.19F;
      this.quadSize = baseSize * styleSize(this.style, wisp) * options.scale()
         * (float)Math.sqrt(this.stretch) * (0.82F + this.random.nextFloat() * 0.36F);
      this.startAlpha = Mth.clamp(options.alpha() * alphaFromColor(options.color()), 0.0F, 1.0F);
      this.alpha = this.startAlpha;
      this.rCol = ARGB.redFloat(options.color());
      this.gCol = ARGB.greenFloat(options.color());
      this.bCol = ARGB.blueFloat(options.color());
      this.gravity = options.gravity();
      this.roll = this.random.nextFloat() * Mth.TWO_PI;
      this.oRoll = this.roll;
      this.setStyleSprite();
   }

   public static ParticleProvider<RenderCoreSoftParticleOptions> provider(SpriteSet sprites, boolean wisp) {
      return (options, level, x, y, z, xSpeed, ySpeed, zSpeed, random) ->
         new RenderCoreSoftParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, options, wisp);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setStyleSprite();
         this.oRoll = this.roll;
         this.roll += this.spin;
         this.setAlpha(this.startAlpha * alphaScale());
      }
   }

   @Override
   public float getQuadSize(float partialTick) {
      float life = Mth.clamp(((float)this.age + partialTick) / (float)Math.max(1, this.lifetime), 0.0F, 1.0F);
      float sizeCurve = this.grows ? Mth.lerp(life, 0.76F, 1.10F) : Mth.lerp(life, 1.03F, 0.78F);
      return super.getQuadSize(partialTick) * sizeCurve;
   }

   @Override
   protected int getLightCoords(float partialTick) {
      return fullbright ? 0xF000F0 : super.getLightCoords(partialTick);
   }

   @Override
   protected Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private static float alphaFromColor(int color) {
      float alpha = ARGB.alphaFloat(color);
      return alpha <= 0.0F ? 1.0F : alpha;
   }

   private float alphaScale() {
      float scale = 1.0F;
      if (fade) {
         float life = (float)this.age / (float)Math.max(1, this.lifetime);
         float fadeIn = Mth.clamp(life * 4.0F, 0.0F, 1.0F);
         float fadeOut = Mth.clamp((1.0F - life) * 2.0F, 0.0F, 1.0F);
         scale *= fadeIn * fadeOut;
      }
      if (flicker > 0.0F) {
         float wave = 0.5F + 0.5F * Mth.sin((this.age * 0.73F) + this.flickerPhase);
         scale *= 1.0F - (this.flicker * 0.35F * wave);
      }
      return Mth.clamp(scale, 0.0F, 1.0F);
   }

   private void setStyleSprite() {
      int maxIndex = this.style.startsWith("wisp_") || this.style.endsWith("_smoke")
         || this.style.endsWith("_breath") || this.style.endsWith("_plume")
         || this.style.endsWith("_haze") || this.style.endsWith("_ribbon")
         || this.style.endsWith("_stream") ? 7 : 8;
      this.setSprite(sprites.get(styleIndex(this.style), maxIndex));
   }

   private static String resolvedStyle(String requested, boolean wisp) {
      if (requested == null || requested.isBlank()) {
         return wisp ? "soft_wisp" : "soft_mote";
      }
      String normalized = requested.trim().toLowerCase(java.util.Locale.ROOT);
      if (wisp) {
         return switch (normalized) {
            case "soft_wisp", "toxic_smoke", "frost_breath", "ash_smoke", "exhaust_plume", "ground_haze",
               "rift_ribbon", "nexus_stream" -> normalized;
            default -> "soft_wisp";
         };
      }
      return switch (normalized) {
         case "soft_mote", "toxic_fleck", "frost_spark", "ash_fleck", "ember", "scan_speck", "rift_pixel",
            "nexus_glyph", "core_glint" -> normalized;
         default -> "soft_mote";
      };
   }

   private static int styleIndex(String style) {
      return switch (style) {
         case "toxic_fleck", "toxic_smoke" -> 1;
         case "frost_spark", "frost_breath" -> 2;
         case "ash_fleck", "ash_smoke" -> 3;
         case "ember", "exhaust_plume" -> 4;
         case "scan_speck", "ground_haze" -> 5;
         case "rift_pixel", "rift_ribbon" -> 6;
         case "nexus_glyph", "nexus_stream" -> 7;
         case "core_glint" -> 8;
         default -> 0;
      };
   }

   private static float styleSize(String style, boolean wisp) {
      if (wisp) {
         return switch (style) {
            case "ground_haze" -> 1.45F;
            case "rift_ribbon", "nexus_stream" -> 1.28F;
            case "frost_breath", "toxic_smoke" -> 1.18F;
            case "ash_smoke" -> 1.12F;
            case "exhaust_plume" -> 1.08F;
            default -> 1.0F;
         };
      }
      return switch (style) {
         case "ash_fleck" -> 0.62F;
         case "toxic_fleck" -> 0.72F;
         case "ember", "frost_spark" -> 0.78F;
         case "rift_pixel" -> 0.86F;
         case "scan_speck" -> 0.76F;
         case "nexus_glyph" -> 0.82F;
         case "core_glint" -> 0.84F;
         default -> 0.88F;
      };
   }
}
