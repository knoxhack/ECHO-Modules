package com.knoxhack.echoconvoyprotocol.client;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyRenderCoreVisuals;
import com.knoxhack.echorendercore.api.VisualContext;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import com.knoxhack.echorendercore.client.DebugVisualOverrides;
import com.knoxhack.echorendercore.client.RenderCoreParticleSpawner;
import com.knoxhack.echorendercore.client.RenderCoreDebugTargets;
import com.knoxhack.echorendercore.client.VisualProfileRenderer;
import com.knoxhack.echorendercore.profile.ParticleProfile;
import com.knoxhack.echorendercore.profile.RenderCoreProfiles;
import com.knoxhack.echorendercore.profile.VisualProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public final class ConvoyRenderCoreVehicleRenderer extends EntityRenderer<ConvoyVehicleEntity, ConvoyVehicleRenderState> {
   private static final Identifier[] TEXTURES = createTextures();
   private static final Identifier[] PROFILES = createProfiles();
   private final ConvoyRenderCoreVehicleModel[] models;

   public ConvoyRenderCoreVehicleRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.models = createModels(context);
      this.shadowRadius = 1.5F;
   }

   @Override
   public ConvoyVehicleRenderState createRenderState() {
      return new ConvoyVehicleRenderState();
   }

   @Override
   public void extractRenderState(ConvoyVehicleEntity entity, ConvoyVehicleRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.yRot = entity.getYRot(partialTick);
      state.kind = entity.kind().ordinal();
      state.damageRatio = ratio(entity.damage(), entity.maxDamage());
      state.fuelRatio = ratio(entity.fuel(), entity.maxFuel());
      state.batteryRatio = ratio(entity.battery(), entity.maxBattery());
      state.cargoRatio = ratio(entity.filledCargoSlots(), entity.cargoSlots());
      state.shieldingRatio = ratio(entity.shieldingPlates(), entity.kind().maxShieldingPlates());
      state.docked = entity.docked();
      state.speed = (float)entity.getDeltaMovement().horizontalDistance();
      state.driven = entity.getControllingPassenger() != null && state.speed > 0.004F;
      state.hasTravelPower = entity.fuel() > 0 || entity.battery() > 0;
      state.renderCoreProfileId = profileFor(entity.kind());
      state.renderCorePartialTick = partialTick;
      state.renderCoreMoving = state.speed > 0.004F;
      state.renderCoreDamaged = state.damageRatio >= 0.65F;
      VisualState mapped = VisualState.byName(ConvoyRenderCoreVisuals.roverVisualStateName(
         state.renderCoreDamaged,
         state.hasTravelPower,
         state.driven,
         state.renderCoreMoving
      ), VisualState.IDLE);
      state.renderCoreVisualState = DebugVisualOverrides.entity(entity.getUUID()).orElse(mapped).name();
      state.renderCoreProgress = Math.max(state.fuelRatio, state.batteryRatio);
      spawnParticles(entity, state);
   }

   @Override
   public void submit(ConvoyVehicleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.translate(0.0F, -1.501F, 0.0F);
      VisualState visualState = VisualState.byName(state.renderCoreVisualState, VisualState.IDLE);
      VisualContext context = new VisualContext(
         state.renderCoreProfileId,
         visualState,
         VisualVariant.DEFAULT,
         state.renderCoreProgress,
         state.ageInTicks,
         state.renderCorePartialTick,
         state.renderCoreMoving,
         state.renderCoreDamaged,
         state.lightCoords
      );
      VisualProfileRenderer.submitEntityModel(
         modelFor(state.kind),
         state,
         poseStack,
         collector,
         context,
         RenderCoreProfiles.visual(state.renderCoreProfileId),
         textureFor(state.kind)
      );
      poseStack.popPose();
      super.submit(state, poseStack, collector, camera);
   }

   private void spawnParticles(ConvoyVehicleEntity entity, ConvoyVehicleRenderState state) {
      VisualProfile profile = RenderCoreProfiles.visual(state.renderCoreProfileId);
      if (profile == null) {
         return;
      }
      VisualState visualState = VisualState.byName(state.renderCoreVisualState, VisualState.IDLE);
      RenderCoreDebugTargets.rememberEntity(entity, profile, new VisualContext(
         state.renderCoreProfileId,
         visualState,
         VisualVariant.DEFAULT,
         state.renderCoreProgress,
         state.ageInTicks,
         state.renderCorePartialTick,
         state.renderCoreMoving,
         state.renderCoreDamaged,
         state.lightCoords
      ));
      if (profile.particleProfile() == null) {
         return;
      }
      ParticleProfile particles = RenderCoreProfiles.particle(profile.particleProfile());
      RenderCoreParticleSpawner.spawnForEntity(
         entity,
         profile,
         particles,
         visualState,
         state.renderCoreMoving,
         state.renderCoreDamaged,
         state.renderCoreProgress
      );
   }

   private ConvoyRenderCoreVehicleModel modelFor(int kind) {
      return models[ConvoyVehicleKind.byId(kind).ordinal()];
   }

   private static Identifier textureFor(int kind) {
      return TEXTURES[ConvoyVehicleKind.byId(kind).ordinal()];
   }

   private static Identifier profileFor(ConvoyVehicleKind kind) {
      return PROFILES[kind.ordinal()];
   }

   private static ConvoyRenderCoreVehicleModel[] createModels(EntityRendererProvider.Context context) {
      ConvoyVehicleKind[] kinds = ConvoyVehicleKind.values();
      ConvoyRenderCoreVehicleModel[] models = new ConvoyRenderCoreVehicleModel[kinds.length];
      for (int i = 0; i < kinds.length; i++) {
         models[i] = new ConvoyRenderCoreVehicleModel(context.bakeLayer(ConvoyVehicleModel.layerLocation(kinds[i])));
      }
      return models;
   }

   private static Identifier[] createTextures() {
      ConvoyVehicleKind[] kinds = ConvoyVehicleKind.values();
      Identifier[] textures = new Identifier[kinds.length];
      for (int i = 0; i < kinds.length; i++) {
         textures[i] = Identifier.fromNamespaceAndPath(
            EchoConvoyProtocol.MODID,
            "textures/entity/rendercore_echo_mobs/" + kinds[i].getSerializedName() + ".png"
         );
      }
      return textures;
   }

   private static Identifier[] createProfiles() {
      ConvoyVehicleKind[] kinds = ConvoyVehicleKind.values();
      Identifier[] profiles = new Identifier[kinds.length];
      for (int i = 0; i < kinds.length; i++) {
         profiles[i] = Identifier.fromNamespaceAndPath(
            EchoConvoyProtocol.MODID,
            "echo_mobs/" + kinds[i].getSerializedName()
         );
      }
      return profiles;
   }

   private static float ratio(int value, int max) {
      if (max <= 0) {
         return 0.0F;
      }
      return Math.max(0.0F, Math.min(1.0F, value / (float)max));
   }
}
