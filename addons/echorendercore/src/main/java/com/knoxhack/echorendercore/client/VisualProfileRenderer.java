package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.profile.VisualLayerKind;
import com.knoxhack.echorendercore.profile.VisualLayerProfile;
import com.knoxhack.echorendercore.profile.VisualMaterial;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.knoxhack.echorendercore.profile.VisualEffectProfile;
import com.knoxhack.echorendercore.profile.VisualEffectTargetScope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class VisualProfileRenderer {
   private VisualProfileRenderer() {
   }

   public static <S extends EntityRenderState> void submitEntityModel(EntityModel<S> model, S state,
         PoseStack poseStack, SubmitNodeCollector collector, VisualContext context, VisualProfile profile,
         Identifier fallbackTexture) {
      Identifier texture = profile == null
         ? fallbackTexture
         : profile.textureFor(context.state(), context.variant());
      if (profile == null) {
         RenderCoreWarnings.missingProfile(context.profileId());
      }
      if (texture == null) {
         texture = fallbackTexture;
      }
      VisualRenderLayer.submitBase(model, state, poseStack, collector, context, texture);
      if (profile == null) {
         return;
      }
      submitEntityLayers(model, state, poseStack, collector, context, profile);
   }

   public static <S extends EntityRenderState> void submitEntityLayers(EntityModel<S> model, S state,
         PoseStack poseStack, SubmitNodeCollector collector, VisualContext context, VisualProfile profile) {
      if (profile == null) {
         return;
      }
      int order = 4;
      if (profile.layers().isEmpty()) {
         if (GlowRenderLayer.shouldGlow(context.state())) {
            GlowRenderLayer.submit(model, state, poseStack, collector, profile.glowTexture());
         }
         submitStateOverlays(model, state, poseStack, collector, context.state(), profile);
         for (Identifier overlay : profile.overlaysFor(context.state())) {
            OverlayRenderLayer.submit(model, state, poseStack, collector, overlay, order++);
         }
      }
      for (VisualLayerProfile layer : profile.layersFor(context.state(), context.variant())) {
         if (layer.kind() == VisualLayerKind.BASE) {
            continue;
         }
         if (layer.texture() == null) {
            continue;
         }
         VisualMaterial material = profile.material(layer.material());
         int color = layer.colorWithAlpha();
         if (material != VisualMaterial.DEFAULT) {
            color = mergeColor(color, material.color(), material.alpha());
         }
         VisualEffectProfile effect = profile.effectFor(layer);
         float layerAlpha = ((color >>> 24) & 0xFF) / 255.0F;
         RenderCoreAdvancedFxPipeline.MaskSubmission mask = RenderCoreAdvancedFxPipeline.submit(
            effect,
            VisualEffectTargetScope.ENTITY,
            layer.texture(),
            color,
            layerAlpha,
            profile.budget()
         );
         color = RenderCoreEffectPipeline.color(color, effect);
         int layerLight = layer.effectiveLight(material, context.packedLight());
         int layerOverlay = layer.effectiveOverlay(material, OverlayTexture.NO_OVERLAY);
         int outlineColor = layer.effectiveOutlineColor(material, state.outlineColor);
         int layerOrder = RenderCoreEffectPipeline.order(Math.max(1, order + layer.effectiveSortOrder(material)), effect);
         if (layer.kind() == VisualLayerKind.GLOW || layer.fullbright(material)) {
            if (!layer.partFilter().isEmpty()) {
               if (mask != null) {
                  VisualPartRenderMask.submit(model, state, poseStack, collector, layer.partFilter(), mask.renderType(),
                     layerOrder, layerLight, layerOverlay, mask.color());
               }
               VisualPartRenderMask.submit(model, state, poseStack, collector, layer.partFilter(), GlowRenderLayer.renderType(layer.texture()),
                  layerOrder, layerLight, layerOverlay, color);
               continue;
            }
            if (mask != null) {
               collector.order(layerOrder).submitModel(model, state, poseStack, mask.renderType(), layerLight, layerOverlay,
                  mask.color(), null, outlineColor, null);
            }
            GlowRenderLayer.submit(model, state, poseStack, collector, layer.texture(), color, layerOrder, layerLight, layerOverlay, outlineColor);
         } else {
            if (!layer.partFilter().isEmpty()) {
               if (mask != null) {
                  VisualPartRenderMask.submit(model, state, poseStack, collector, layer.partFilter(), mask.renderType(),
                     layerOrder, layerLight, layerOverlay, mask.color());
               }
               VisualPartRenderMask.submit(model, state, poseStack, collector, layer.partFilter(), OverlayRenderLayer.renderType(layer.texture()),
                  layerOrder, layerLight, layerOverlay, color);
               order++;
               continue;
            }
            if (mask != null) {
               collector.order(layerOrder).submitModel(model, state, poseStack, mask.renderType(), layerLight, layerOverlay,
                  mask.color(), null, outlineColor, null);
            }
            OverlayRenderLayer.submit(model, state, poseStack, collector, layer.texture(), layerOrder, color, layerLight, layerOverlay, outlineColor);
            order++;
         }
      }
   }

   private static <S extends EntityRenderState> void submitStateOverlays(EntityModel<S> model, S state,
         PoseStack poseStack, SubmitNodeCollector collector, VisualState visualState, VisualProfile profile) {
      if (visualState == VisualState.DAMAGED) {
         OverlayRenderLayer.submit(model, state, poseStack, collector, profile.damagedOverlayTexture(), 2);
      } else if (visualState == VisualState.CORRUPTED) {
         OverlayRenderLayer.submit(model, state, poseStack, collector, profile.corruptedOverlayTexture(), 2);
      } else if (visualState == VisualState.ACTIVE || visualState == VisualState.WORKING || visualState == VisualState.SCANNING) {
         OverlayRenderLayer.submit(model, state, poseStack, collector, profile.activeOverlayTexture(), 2);
      }
   }

   private static int mergeColor(int layerColor, int materialColor, float materialAlpha) {
      int alpha = Math.round(((layerColor >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, materialAlpha)));
      int r = (((layerColor >>> 16) & 0xFF) * ((materialColor >>> 16) & 0xFF)) / 255;
      int g = (((layerColor >>> 8) & 0xFF) * ((materialColor >>> 8) & 0xFF)) / 255;
      int b = ((layerColor & 0xFF) * (materialColor & 0xFF)) / 255;
      return (alpha << 24) | (r << 16) | (g << 8) | b;
   }
}
