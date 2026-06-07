package com.knoxhack.echoagriculturereclamation.entity;

import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PollinatorDroneEntity extends Vex {
   private static final EntityDataAccessor<String> DATA_DIALOG_STATUS =
      SynchedEntityData.defineId(PollinatorDroneEntity.class, EntityDataSerializers.STRING);
   private static final int DEFAULT_SEARCH_RADIUS = 24;
   private BlockPos homeDock = BlockPos.ZERO;
   private BlockPos targetPos = BlockPos.ZERO;
   private int serviceCooldown;
   private int serviceCount;
   private String status = "idle";

   public PollinatorDroneEntity(EntityType<? extends Vex> type, Level level) {
      super(type, level);
      setPersistenceRequired();
      setNoGravity(true);
      xpReward = 0;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Vex.createAttributes()
         .add(Attributes.MAX_HEALTH, 14.0)
         .add(Attributes.ATTACK_DAMAGE, 0.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_DIALOG_STATUS, "Pollinator Drone idle.");
   }

   @Override
   protected void registerGoals() {
   }

   public void configureDock(BlockPos dock) {
      homeDock = dock == null ? BlockPos.ZERO : dock.immutable();
      targetPos = BlockPos.ZERO;
      serviceCooldown = 0;
      status = "deployed";
      setCustomName(Component.literal("Pollinator Drone"));
      setCustomNameVisible(false);
      syncDialogStatus();
   }

   public BlockPos homeDock() {
      return homeDock;
   }

   public BlockPos targetPos() {
      return targetPos;
   }

   public int serviceCooldown() {
      return serviceCooldown;
   }

   public int serviceCount() {
      return serviceCount;
   }

   public String statusLine() {
      int targets = level() == null ? 0 : ReclamationProgress.pollinationServiceTargets(level(), homeDock);
      return statusLabel() + ", " + countLabel(targets, "service target")
         + ", " + countLabel(serviceCount, "service visit");
   }

   public String dialogCardStatus() {
      return cleanStatus(entityData.get(DATA_DIALOG_STATUS));
   }

   public static PollinatorDroneEntity deployOrFind(ServerLevel level, BlockPos dock) {
      PollinatorDroneEntity existing = cleanupDuplicateDrones(level, dock, null);
      if (existing != null) {
         return existing;
      }
      PollinatorDroneEntity drone = ModEntities.POLLINATOR_DRONE.get().create(level, EntitySpawnReason.EVENT);
      if (drone == null) {
         throw new IllegalStateException("Unable to create Agriculture Pollinator Drone entity.");
      }
      drone.configureDock(dock);
      drone.setPos(dock.getX() + 0.5D, dock.getY() + 1.25D, dock.getZ() + 0.5D);
      level.addFreshEntity(drone);
      return drone;
   }

   public static int recallDrones(ServerLevel level, BlockPos dock) {
      int recalled = 0;
      for (PollinatorDroneEntity drone : boundDrones(level, dock)) {
         drone.status = "recalled";
         drone.discard();
         recalled++;
      }
      return recalled;
   }

   public static int boundDroneCount(Level level, BlockPos dock) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return 0;
      }
      return boundDrones(serverLevel, dock).size();
   }

   public static PollinatorDroneEntity cleanupDuplicateDrones(ServerLevel level, BlockPos dock, PollinatorDroneEntity preferred) {
      List<PollinatorDroneEntity> drones = boundDrones(level, dock);
      if (drones.isEmpty()) {
         return null;
      }
      PollinatorDroneEntity keeper = preferred != null && drones.contains(preferred) ? preferred : drones.get(0);
      for (PollinatorDroneEntity drone : drones) {
         if (drone != keeper) {
            drone.status = "duplicate_recalled";
            drone.discard();
         }
      }
      return keeper;
   }

   public static List<PollinatorDroneEntity> boundDrones(ServerLevel level, BlockPos dock) {
      int searchRadius = Math.max(DEFAULT_SEARCH_RADIUS,
         ReclamationContent.machines().pollinatorDroneHomeRadius() + ReclamationContent.machines().pollinatorDroneServiceRadius() + 4);
      AABB search = new AABB(dock).inflate(searchRadius);
      Map<UUID, PollinatorDroneEntity> drones = new LinkedHashMap<>();
      for (PollinatorDroneEntity drone : level.getEntitiesOfClass(PollinatorDroneEntity.class, search, drone -> validBoundDrone(drone, dock, search))) {
         drones.putIfAbsent(drone.getUUID(), drone);
      }
      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof PollinatorDroneEntity drone && validBoundDrone(drone, dock, search)) {
            drones.putIfAbsent(drone.getUUID(), drone);
         }
      }
      return new ArrayList<>(drones.values())
         .stream()
         .sorted(Comparator.comparingInt(PollinatorDroneEntity::getId)
            .thenComparingDouble(drone -> drone.distanceToDock(dock))
            .thenComparing(drone -> drone.getUUID().toString()))
         .toList();
   }

   private static boolean validBoundDrone(PollinatorDroneEntity drone, BlockPos dock, AABB search) {
      return drone != null
         && !drone.isRemoved()
         && dock.equals(drone.homeDock())
         && search.intersects(drone.getBoundingBox());
   }

   @Override
   public boolean isAlwaysTicking() {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      setNoGravity(true);
      setTarget(null);
      if (level().isClientSide()) {
         return;
      }
      if (!(level() instanceof ServerLevel serverLevel)) {
         return;
      }
      if (!dockPresent(serverLevel)) {
         status = "dock_lost";
         syncDialogStatus();
         discard();
         return;
      }
      if (tickCount % 40 == 0) {
         cleanupDuplicateDrones(serverLevel, homeDock, this);
      }
      if (distanceToDock(homeDock) > ReclamationContent.machines().pollinatorDroneHomeRadius() * 2.5D) {
         targetPos = BlockPos.ZERO;
      }
      if (serviceCooldown > 0) {
         serviceCooldown--;
      }

      BlockPos destination = homeDock.above();
      Optional<BlockPos> target = Optional.empty();
      if (serviceCooldown <= 0) {
         target = serviceTarget(serverLevel);
         if (target.isPresent()) {
            targetPos = target.get();
            destination = targetPos.above();
            status = "approaching";
         } else {
            targetPos = BlockPos.ZERO;
            status = ReclamationProgress.pollinationTargets(serverLevel, homeDock) > 0 ? "surveying" : "idle";
         }
      } else if (!targetPos.equals(BlockPos.ZERO)) {
         destination = targetPos.above();
      } else {
         status = "cooling";
      }

      flyToward(destination);
      if (serviceCooldown <= 0 && target.isPresent() && distanceTo(target.get()) <= 1.5D) {
         boolean serviced = ReclamationProgress.servicePollinationTarget(
            serverLevel,
            target.get(),
            ReclamationContent.machines().pollinatorDroneGrowthBonus()
         );
         if (serviced) {
            serviceCount++;
            status = "servicing";
         } else {
            ReclamationProgress.GreenhouseContext context = ReclamationProgress.greenhouseContext(serverLevel, target.get());
            status = context.established() ? "surveying" : "awaiting_zone";
         }
         targetPos = BlockPos.ZERO;
         serviceCooldown = ReclamationContent.machines().pollinatorDroneServiceTicks();
      }
      if (tickCount % 10 == 0) {
         syncDialogStatus();
      }
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.SUCCESS;
      }
      if (player.isShiftKeyDown()) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Pollinator Drone recalled to dock memory."));
         status = "recalled";
         syncDialogStatus();
         discard();
      } else {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Pollinator Drone " + statusLine()
            + ". Home dock " + homeDock.getX() + "," + homeDock.getY() + "," + homeDock.getZ() + "."));
      }
      return InteractionResult.SUCCESS_SERVER;
   }

   private boolean dockPresent(ServerLevel level) {
      return homeDock != null && !homeDock.equals(BlockPos.ZERO) && level.getBlockState(homeDock).is(ModBlocks.POLLINATOR_DRONE_DOCK.get());
   }

   private Optional<BlockPos> serviceTarget(ServerLevel level) {
      return ReclamationProgress.pollinationTargetPositions(level, homeDock)
         .stream()
         .filter(pos -> ReclamationProgress.canReceivePollinationService(level, pos))
         .min(Comparator.comparingDouble(this::distanceTo));
   }

   private void flyToward(BlockPos destination) {
      Vec3 goal = new Vec3(destination.getX() + 0.5D, destination.getY() + 0.85D, destination.getZ() + 0.5D);
      Vec3 delta = goal.subtract(position());
      double distance = Math.max(0.001D, delta.length());
      double speed = Math.min(0.32D, 0.10D + distance * 0.018D);
      Vec3 motion = delta.normalize().scale(speed);
      setDeltaMovement(motion);
      move(MoverType.SELF, motion);
      setYRot((float)(Math.atan2(motion.z, motion.x) * 57.2957763671875D) - 90.0F);
      setOldPosAndRot();
   }

   private double distanceTo(BlockPos pos) {
      return position().distanceTo(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.85D, pos.getZ() + 0.5D));
   }

   private double distanceToDock(BlockPos dock) {
      return position().distanceTo(new Vec3(dock.getX() + 0.5D, dock.getY() + 1.25D, dock.getZ() + 0.5D));
   }

   private String statusLabel() {
      return status == null || status.isBlank() ? "idle" : status.replace('_', ' ');
   }

   private void syncDialogStatus() {
      setDialogStatus(statusLine());
   }

   private void setDialogStatus(String value) {
      String clean = cleanStatus(value);
      if (!clean.equals(entityData.get(DATA_DIALOG_STATUS))) {
         entityData.set(DATA_DIALOG_STATUS, clean);
      }
   }

   private static String cleanStatus(String value) {
      return value == null || value.isBlank() ? "idle" : value.replace('_', ' ').trim();
   }

   private static String countLabel(int count, String noun) {
      return count + " " + noun + (count == 1 ? "" : "s");
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("home_x", homeDock.getX());
      output.putInt("home_y", homeDock.getY());
      output.putInt("home_z", homeDock.getZ());
      output.putInt("target_x", targetPos.getX());
      output.putInt("target_y", targetPos.getY());
      output.putInt("target_z", targetPos.getZ());
      output.putInt("service_cooldown", serviceCooldown);
      output.putInt("service_count", serviceCount);
      output.putString("status", statusLabel());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      homeDock = new BlockPos(input.getIntOr("home_x", 0), input.getIntOr("home_y", 0), input.getIntOr("home_z", 0));
      targetPos = new BlockPos(input.getIntOr("target_x", 0), input.getIntOr("target_y", 0), input.getIntOr("target_z", 0));
      serviceCooldown = Math.max(0, input.getIntOr("service_cooldown", 0));
      serviceCount = Math.max(0, input.getIntOr("service_count", 0));
      status = input.getStringOr("status", "idle");
      setDialogStatus(statusLabel());
   }
}
