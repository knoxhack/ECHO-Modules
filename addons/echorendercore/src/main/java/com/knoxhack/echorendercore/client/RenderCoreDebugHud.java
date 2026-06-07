package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.IAdvancedVisualBlockEntity;
import com.knoxhack.echorendercore.api.IAdvancedVisualEntity;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class RenderCoreDebugHud {
   private RenderCoreDebugHud() {
   }

   public static void render(GuiGraphicsExtractor graphics) {
      if (!DebugVisualOverrides.hudEnabled()) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         return;
      }
      Font font = minecraft.font;
      int x = 8;
      int y = 8;
      graphics.fill(x - 4, y - 4, x + 330, y + 118, 0xAA061018);
      line(graphics, font, "RenderCore HUD", x, y, 0xFF66E8FF);
      y += 10;
      var loaded = RenderCoreProfiles.loaded();
      var metrics = loaded.cacheMetrics();
      line(graphics, font, "profiles " + metrics.visualProfileCount() + " / anim " + metrics.animationProfileCount()
         + " / particles " + metrics.particleProfileCount(), x, y, 0xFFE6F8FF);
      y += 10;
      line(graphics, font, "json " + metrics.loadedJsonCount() + "/" + metrics.discoveredJsonCount()
         + " failed " + metrics.failedJsonCount() + " / W:" + metrics.validationWarningCount()
         + " E:" + metrics.validationErrorCount() + " P:" + metrics.performanceWarningCount(), x, y, 0xFFE6F8FF);
      y += 10;
      TargetInfo target = target(minecraft);
      line(graphics, font, target.line(), x, y, target.color());
      y += 10;
      line(graphics, font, target.detail(), x, y, target.color());
      y += 10;
      line(graphics, font, "anchors " + (DebugVisualOverrides.anchorsEnabled() ? "on" : "off")
         + " / missing parts " + (DebugVisualOverrides.missingPartWarnings() ? "on" : "off"), x, y, 0xFF8DB3C7);
      y += 10;
      line(graphics, font, RenderCoreEffectPipeline.statusLine() + " / active " + RenderCoreAdvancedFxPipeline.lastEffectCount()
         + " / passes " + RenderCoreAdvancedFxPipeline.lastPassCount(), x, y, 0xFFB76DFF);
      y += 10;
      line(graphics, font, "fx " + RenderCoreAdvancedFxPipeline.modeLine()
         + " / masks " + RenderCoreAdvancedFxPipeline.lastMaskSubmissionCount()
         + " skipped " + RenderCoreAdvancedFxPipeline.lastSkippedSubmissions()
         + " channels " + RenderCoreAdvancedFxPipeline.lastChannelCount()
         + " x" + RenderCoreAdvancedFxPipeline.lastDownscale()
         + " cost " + RenderCoreAdvancedFxPipeline.lastBloomCost(), x, y, 0xFFB76DFF);
      y += 10;
      if (!RenderCoreAdvancedFxPipeline.fallbackReason().isBlank()) {
         line(graphics, font, RenderCoreAdvancedFxPipeline.fallbackReason(), x, y, 0xFFFF86C8);
         y += 10;
      }
      if (RenderCoreEffectPipeline.advancedFxEnabled() && !RenderCoreEffectPipeline.advancedFxAvailable()) {
         line(graphics, font, RenderCoreAdvancedFxPipeline.unavailableReason(), x, y, 0xFFFF86C8);
      }
   }

   private static TargetInfo target(Minecraft minecraft) {
      var debugTarget = RenderCoreDebugTargets.lookedAt(minecraft);
      if (debugTarget.isPresent()) {
         RenderCoreDebugTargets.DebugTarget target = debugTarget.get();
         return new TargetInfo(
            "target " + target.profileId() + " state " + target.state().name(),
            "variant " + target.variant().id() + " layers " + target.activeLayers() + " anchors " + target.anchors()
               + " surface " + target.surfaceType()
               + " particles " + (target.particleProfileId() == null ? "none" : target.particleProfileId())
               + " emitters " + target.activeEmitters() + "/" + target.skippedEmitters()
               + " " + target.fallbackStatus() + " warnings " + target.validationWarnings(),
            0xFFE6F8FF
         );
      }
      HitResult hit = minecraft.hitResult;
      if (minecraft.level == null || hit == null) {
         return new TargetInfo("target none", "", 0xFF8DB3C7);
      }
      if (hit instanceof EntityHitResult entityHit) {
         Entity entity = entityHit.getEntity();
         if (entity instanceof IAdvancedVisualEntity visual) {
            VisualState state = DebugVisualOverrides.entity(entity.getUUID()).orElse(visual.visualState());
            return new TargetInfo("entity " + visual.visualProfileId() + " state " + state.name(), "variant " + visual.visualVariant().id(), 0xFFE6F8FF);
         }
         return new TargetInfo("entity " + entity.getName().getString(), "", 0xFF8DB3C7);
      }
      if (hit instanceof BlockHitResult blockHit) {
         BlockEntity blockEntity = minecraft.level.getBlockEntity(blockHit.getBlockPos());
         if (blockEntity instanceof IAdvancedVisualBlockEntity visual) {
            VisualState state = DebugVisualOverrides.block(minecraft.level, blockHit.getBlockPos()).orElse(visual.visualState());
            return new TargetInfo("block " + visual.visualProfileId() + " state " + state.name(), "variant " + visual.visualVariant().id(), 0xFFE6F8FF);
         }
         return new TargetInfo("block " + blockHit.getBlockPos().toShortString(), "", 0xFF8DB3C7);
      }
      return new TargetInfo("target unsupported", "", 0xFF8DB3C7);
   }

   private static void line(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
      graphics.text(font, font.plainSubstrByWidth(text, 322), x, y, color, false);
   }

   private record TargetInfo(String line, String detail, int color) {
   }
}
