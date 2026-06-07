package com.knoxhack.echoconvoyprotocol.item;

import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.knoxhack.echoconvoyprotocol.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class VehicleKitItem extends Item {
   private final ConvoyVehicleKind kind;

   public VehicleKitItem(ConvoyVehicleKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return InteractionResult.SUCCESS;
      }
      EntityType<ConvoyVehicleEntity> type = ModEntities.typeFor(kind);
      ConvoyVehicleEntity vehicle = type.create(serverLevel, EntitySpawnReason.EVENT);
      if (vehicle == null) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Vehicle registry offline."));
         return InteractionResult.CONSUME;
      }
      Vec3 spawn = findDeploymentPosition(serverLevel, player, vehicle);
      if (spawn == null) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Clear a flat staging area before deploying the " + kind.displayName() + "."));
         return InteractionResult.CONSUME;
      }
      vehicle.setPos(spawn.x, spawn.y, spawn.z);
      vehicle.setYRot(player.getYRot());
      vehicle.setOldPosAndRot();
      if (!serverLevel.addFreshEntity(vehicle)) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Vehicle deployment failed in this loaded area."));
         return InteractionResult.CONSUME;
      }
      ItemStack stack = player.getItemInHand(hand);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      player.sendSystemMessage(Component.literal("ECHO CONVOY // " + kind.displayName() + " deployed. Right-click to claim and drive."));
      return InteractionResult.SUCCESS_SERVER;
   }

   @Nullable
   private static Vec3 findDeploymentPosition(ServerLevel level, Player player, ConvoyVehicleEntity vehicle) {
      Vec3 forward = horizontalForward(player);
      double baseDistance = Math.max(1.75D, player.getBbWidth() * 0.5D + vehicle.getBbWidth() * 0.5D + 0.75D);
      BlockPos playerPos = player.blockPosition();
      for (int step = 0; step < 6; step++) {
         Vec3 center = player.position().add(forward.scale(baseDistance + step * 0.75D));
         for (int dy = 1; dy >= -2; dy--) {
            BlockPos feet = BlockPos.containing(center.x, playerPos.getY() + dy, center.z);
            BlockPos ground = feet.below();
            if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
               continue;
            }
            vehicle.setPos(center.x, feet.getY(), center.z);
            vehicle.setYRot(player.getYRot());
            vehicle.setOldPosAndRot();
            if (!vehicle.getBoundingBox().intersects(player.getBoundingBox())
               && level.noBlockCollision(vehicle, vehicle.getBoundingBox())
               && level.noBorderCollision(vehicle, vehicle.getBoundingBox())) {
               return new Vec3(center.x, feet.getY(), center.z);
            }
         }
      }
      return null;
   }

   private static Vec3 horizontalForward(Player player) {
      Vec3 look = player.getLookAngle();
      Vec3 forward = new Vec3(look.x, 0.0D, look.z);
      if (forward.lengthSqr() > 1.0E-5D) {
         return forward.normalize();
      }
      float yaw = player.getYRot() * Mth.DEG_TO_RAD;
      return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
   }
}
