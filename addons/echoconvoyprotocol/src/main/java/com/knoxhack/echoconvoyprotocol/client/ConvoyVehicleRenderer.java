package com.knoxhack.echoconvoyprotocol.client;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class ConvoyVehicleRenderer extends EntityRenderer<ConvoyVehicleEntity, ConvoyVehicleRenderState> {
   private static final Identifier[] TEXTURES = createTextures();
   private final ConvoyVehicleModel[] models;

   public ConvoyVehicleRenderer(EntityRendererProvider.Context context) {
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
   }

   @Override
   public void submit(ConvoyVehicleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.translate(0.0F, -1.501F, 0.0F);
      collector.submitModel(modelFor(state.kind), state, poseStack, textureFor(state.kind), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
      poseStack.popPose();
      super.submit(state, poseStack, collector, camera);
   }

   private ConvoyVehicleModel modelFor(int kind) {
      return models[ConvoyVehicleKind.byId(kind).ordinal()];
   }

   private static Identifier textureFor(int kind) {
      return TEXTURES[ConvoyVehicleKind.byId(kind).ordinal()];
   }

   private static ConvoyVehicleModel[] createModels(EntityRendererProvider.Context context) {
      ConvoyVehicleKind[] kinds = ConvoyVehicleKind.values();
      ConvoyVehicleModel[] models = new ConvoyVehicleModel[kinds.length];
      for (int i = 0; i < kinds.length; i++) {
         models[i] = new ConvoyVehicleModel(context.bakeLayer(ConvoyVehicleModel.layerLocation(kinds[i])));
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

   private static float ratio(int value, int max) {
      if (max <= 0) {
         return 0.0F;
      }
      return Math.max(0.0F, Math.min(1.0F, value / (float)max));
   }
}
