package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.profile.VisualLayerKind;
import com.knoxhack.echorendercore.profile.VisualLayerProfile;
import com.knoxhack.echorendercore.profile.VisualEffectProfile;
import com.knoxhack.echorendercore.profile.VisualEffectTargetScope;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BakedBlockPartMask {
   private static final int[] NO_TINTS = new int[0];

   private BakedBlockPartMask() {
   }

   public static boolean submitLayer(BlockState blockState, VisualProfile profile, VisualLayerProfile layer,
         PoseStack poseStack, SubmitNodeCollector collector, VisualContext context, int order) {
      if (layer == null || layer.kind() == VisualLayerKind.BASE) {
         return false;
      }
      List<BlockStateModelPart> parts = partsForLayer(blockState, profile, layer);
      if (parts.isEmpty()) {
         return false;
      }
      VisualEffectProfile effect = profile.effectFor(layer);
      RenderCoreAdvancedFxPipeline.MaskSubmission mask = RenderCoreAdvancedFxPipeline.submit(
         effect,
         VisualEffectTargetScope.BLOCK,
         TextureAtlas.LOCATION_BLOCKS,
         layer.colorWithAlpha(),
         layer.alpha(),
         profile.budget()
      );
      if (mask != null) {
         collector.order(RenderCoreEffectPipeline.order(order, effect)).submitBlockModel(
            poseStack,
            mask.renderType(),
            parts,
            NO_TINTS,
            context.packedLight(),
            OverlayTexture.NO_OVERLAY,
            0
         );
      }
      collector.order(RenderCoreEffectPipeline.order(order, effect)).submitBlockModel(
         poseStack,
         renderType(layer),
         parts,
         NO_TINTS,
         context.packedLight(),
         OverlayTexture.NO_OVERLAY,
         0
      );
      return true;
   }

   public static List<BlockStateModelPart> partsForLayer(BlockState blockState, VisualProfile profile, VisualLayerProfile layer) {
      if (blockState == null || profile == null) {
         return List.of();
      }
      if (layer.partFilter().isEmpty()) {
         return profile.blockParts().isEmpty() ? List.of() : BakedBlockPartResolver.collect(blockState);
      }
      Map<String, List<BlockStateModelPart>> aliases = BakedBlockPartResolver.resolve(blockState, profile);
      ArrayList<BlockStateModelPart> selected = new ArrayList<>();
      for (String part : layer.partFilter()) {
         List<BlockStateModelPart> resolved = aliases.get(part);
         if (resolved == null || resolved.isEmpty()) {
            if (DebugVisualOverrides.missingPartWarnings()) {
               RenderCoreWarnings.warn("RenderCore masked block part '" + part + "' did not match collected baked model parts.");
            }
            continue;
         }
         selected.addAll(resolved);
      }
      return selected;
   }

   private static RenderType renderType(VisualLayerProfile layer) {
      if (layer.kind() == VisualLayerKind.GLOW || layer.emissive()) {
         return RenderTypes.cutoutMovingBlock();
      }
      return RenderTypes.translucentMovingBlock();
   }
}
