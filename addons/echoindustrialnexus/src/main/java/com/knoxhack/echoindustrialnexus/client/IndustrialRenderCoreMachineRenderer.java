package com.knoxhack.echoindustrialnexus.client;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.integration.IndustrialRenderCoreVisuals;
import com.knoxhack.echorendercore.api.RenderCoreBlockVisualHost;
import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import com.knoxhack.echorendercore.client.AdvancedBlockEntityVisualRenderer;
import com.knoxhack.echorendercore.client.DebugVisualOverrides;
import com.knoxhack.echorendercore.client.RenderCoreBlockVisuals;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class IndustrialRenderCoreMachineRenderer
      extends AdvancedBlockEntityVisualRenderer<IndustrialMachineBlockEntity, IndustrialRenderCoreMachineRenderer.MachineVisualRenderState> {
   private static final Identifier PROFILE = Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "industrial_machine");

   public IndustrialRenderCoreMachineRenderer(BlockEntityRendererProvider.Context context) {
   }

   @Override
   public MachineVisualRenderState createRenderState() {
      return new MachineVisualRenderState();
   }

   @Override
   public void extractRenderState(IndustrialMachineBlockEntity machine, MachineVisualRenderState state, float partialTick,
         Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
      state.level = machine.getLevel();
      state.pos = machine.getBlockPos();
      state.blockState = machine.getBlockState();
      state.origin = Vec3.atCenterOf(machine.getBlockPos());
      state.ageInTicks = machine.getLevel() == null ? partialTick : machine.getLevel().getGameTime() + partialTick;
      state.profileId = PROFILE;
      state.visualState = VisualState.byName(
         IndustrialRenderCoreVisuals.visualStateName(machine.machineStatus(), machine.heatLevel(), machine.getFluxStored()),
         VisualState.IDLE
      );
      state.visualState = DebugVisualOverrides.block(machine.getLevel(), machine.getBlockPos()).orElse(state.visualState);
      state.variant = VisualVariant.of(IndustrialRenderCoreVisuals.variantId(machine.kind()));
      state.progress = IndustrialRenderCoreVisuals.progress(
         machine.progressTicks(),
         machine.maxProgressTicks(),
         machine.getFluxStored(),
         machine.getMaxFluxStored()
      );
      state.moving = state.visualState == VisualState.ACTIVE || state.visualState == VisualState.WORKING;
      state.damaged = state.visualState == VisualState.OVERHEATED
         || state.visualState == VisualState.FAILED
         || state.visualState == VisualState.CORRUPTED;
      state.partialTick = partialTick;
   }

   @Override
   public void submit(MachineVisualRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      VisualContext context = new VisualContext(
         state.profileId,
         state.visualState,
         state.variant,
         state.progress,
         state.ageInTicks,
         state.partialTick,
         state.moving,
         state.damaged,
         state.lightCoords
      );
      RenderCoreBlockVisuals.RenderData data = RenderCoreBlockVisuals.resolve(
         state.level,
         state.pos,
         state.blockState,
         host(state),
         state.partialTick,
         state.lightCoords
      );
      VisualProfile visualProfile = data == null ? profile(context) : data.visualProfile();
      if (visualProfile == null) {
         return;
      }
      submitBlockModelLayers(state.blockState, poseStack, collector, data == null ? context : data.context(), visualProfile);
      RenderCoreBlockVisuals.spawnParticlesOncePerTick(data);
   }

   private static RenderCoreBlockVisualHost host(MachineVisualRenderState state) {
      return new RenderCoreBlockVisualHost() {
         @Override
         public Identifier visualProfileId() {
            return state.profileId;
         }

         @Override
         public VisualState visualState() {
            return state.visualState;
         }

         @Override
         public VisualVariant visualVariant() {
            return state.variant;
         }

         @Override
         public float visualProgress() {
            return state.progress;
         }

         @Override
         public boolean visualMoving() {
            return state.moving;
         }

         @Override
         public boolean visualDamaged() {
            return state.damaged;
         }

         @Override
         public String visualFallbackStatus() {
            return "rendercore_native";
         }
      };
   }

   public static final class MachineVisualRenderState extends BlockEntityRenderState {
      private Level level;
      private BlockPos pos = BlockPos.ZERO;
      private BlockState blockState;
      private Vec3 origin = Vec3.ZERO;
      private Identifier profileId = PROFILE;
      private VisualState visualState = VisualState.IDLE;
      private VisualVariant variant = VisualVariant.DEFAULT;
      private float ageInTicks;
      private float progress;
      private float partialTick;
      private boolean moving;
      private boolean damaged;
   }
}
