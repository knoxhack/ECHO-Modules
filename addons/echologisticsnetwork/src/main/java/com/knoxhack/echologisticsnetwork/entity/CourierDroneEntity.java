package com.knoxhack.echologisticsnetwork.entity;

import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.knoxhack.echologisticsnetwork.integration.LogisticsMissionHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CourierDroneEntity extends Vex {
   private static final EntityDataAccessor<String> DATA_DIALOG_STATUS =
      SynchedEntityData.defineId(CourierDroneEntity.class, EntityDataSerializers.STRING);
   private static final int PAYLOAD_SLOTS = 27;
   private final NonNullList<ItemStack> payload = NonNullList.withSize(PAYLOAD_SLOTS, ItemStack.EMPTY);
   private UUID jobId = UUID.randomUUID();
   @Nullable
   private UUID owner;
   private String networkId = "global";
   private BlockPos sourceDock = BlockPos.ZERO;
   private BlockPos targetPos = BlockPos.ZERO;
   private Identifier presetId = Identifier.fromNamespaceAndPath("echologisticsnetwork", "unknown");
   private int deliveryTicks = 160;
   private int age;
   private long createdTick;
   private long etaTick;
   private String status = "in_transit";
   private boolean returning;
   private boolean delivered;

   public CourierDroneEntity(EntityType<? extends Vex> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.setNoGravity(true);
      this.xpReward = 0;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Vex.createAttributes()
         .add(Attributes.MAX_HEALTH, 16.0)
         .add(Attributes.ATTACK_DAMAGE, 0.0)
         .add(Attributes.MOVEMENT_SPEED, 0.28);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_DIALOG_STATUS, "Courier in transit.");
   }

   @Override
   protected void registerGoals() {
   }

   public void configureDelivery(UUID owner, BlockPos sourceDock, BlockPos targetPos, Identifier presetId, List<ItemStack> stacks, int deliveryTicks) {
      configureDelivery(UUID.randomUUID(), owner, "global", sourceDock, targetPos, presetId, stacks, deliveryTicks);
   }

   public void configureDelivery(UUID jobId, UUID owner, String networkId, BlockPos sourceDock, BlockPos targetPos, Identifier presetId, List<ItemStack> stacks, int deliveryTicks) {
      this.jobId = jobId == null ? UUID.randomUUID() : jobId;
      this.owner = owner;
      this.networkId = networkId == null || networkId.isBlank() ? "global" : networkId;
      this.sourceDock = sourceDock == null ? BlockPos.ZERO : sourceDock.immutable();
      this.targetPos = targetPos == null ? this.sourceDock : targetPos.immutable();
      this.presetId = presetId == null ? Identifier.fromNamespaceAndPath("echologisticsnetwork", "unknown") : presetId;
      this.deliveryTicks = Math.max(40, deliveryTicks);
      this.age = 0;
      this.createdTick = level().getGameTime();
      this.etaTick = this.createdTick + this.deliveryTicks;
      this.status = "in_transit";
      this.returning = false;
      this.delivered = false;
      for (int i = 0; i < payload.size(); i++) {
         payload.set(i, ItemStack.EMPTY);
      }
      int slot = 0;
      for (ItemStack stack : stacks == null ? List.<ItemStack>of() : stacks) {
         if (!stack.isEmpty() && slot < payload.size()) {
            payload.set(slot++, stack.copy());
         }
      }
      syncDialogStatus();
   }

   public String networkId() {
      return networkId == null || networkId.isBlank() ? "global" : networkId;
   }

   public LogisticsNetworkService.DeliveryJob deliveryJob() {
      return new LogisticsNetworkService.DeliveryJob(jobId, owner, sourceDock, targetPos, presetId, payloadStacks(), statusLine(), createdTick, etaTick);
   }

   public String dialogCardStatus() {
      return cleanStatus(entityData.get(DATA_DIALOG_STATUS));
   }

   public boolean cancelToDock(String reason) {
      if (level().isClientSide() || payloadStacks().isEmpty()) {
         return false;
      }
      failRecoverably(reason == null || reason.isBlank() ? "cancelled" : reason);
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      this.setNoGravity(true);
      if (level().isClientSide()) {
         return;
      }
      age++;
      if (payloadStacks().isEmpty()) {
         status = delivered ? "complete" : "empty_payload";
         syncDialogStatus();
         discard();
         return;
      }
      if (age > deliveryTicks * 4) {
         failRecoverably("timed out");
         return;
      }
      BlockPos destination = returning ? sourceDock : targetPos;
      flyToward(destination);
      if (distanceTo(destination) <= 1.4D) {
         if (returning) {
            syncDialogStatus();
            discard();
         } else {
            attemptDelivery();
         }
      }
      if (age % 10 == 0) {
         syncDialogStatus();
      }
   }

   private void flyToward(BlockPos destination) {
      Vec3 goal = new Vec3(destination.getX() + 0.5D, destination.getY() + 1.2D, destination.getZ() + 0.5D);
      Vec3 delta = goal.subtract(position());
      double distance = Math.max(0.001D, delta.length());
      double speed = Math.min(0.34D, 0.12D + distance * 0.012D);
      Vec3 motion = delta.normalize().scale(speed);
      setDeltaMovement(motion);
      move(MoverType.SELF, motion);
      setYRot((float)(Math.atan2(motion.z, motion.x) * 57.2957763671875D) - 90.0F);
      setOldPosAndRot();
   }

   private double distanceTo(BlockPos pos) {
      return position().distanceTo(new Vec3(pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D));
   }

   private void attemptDelivery() {
      List<ItemStack> stacks = payloadStacks();
      if (LogisticsNetworkService.deliverPayload(level(), targetPos, stacks)) {
         clearPayload();
         delivered = true;
         status = "delivered";
         LogisticsNetworkService.recordDeliveryStatus(level(), sourceDock, targetPos, "Delivery " + shortJob() + " delivered: " + presetId.getPath().replace('_', ' '));
         notifyOwner("Courier delivered " + presetId.getPath().replace('_', ' ') + ".");
         recordDeliveryMission();
         returning = true;
         syncDialogStatus();
      } else {
         failRecoverably("target inventory unavailable");
      }
   }

   private void failRecoverably(String reason) {
      List<ItemStack> stacks = payloadStacks();
      status = "recovering";
      LogisticsNetworkService.recoverPayload(level(), sourceDock, stacks);
      clearPayload();
      LogisticsNetworkService.recordDeliveryStatus(level(), sourceDock, targetPos, "Delivery " + shortJob() + " recovered: " + reason);
      notifyOwner("Courier recovered payload after " + reason + ".");
      syncDialogStatus();
      discard();
   }

   private List<ItemStack> payloadStacks() {
      List<ItemStack> stacks = new ArrayList<>();
      for (ItemStack stack : payload) {
         if (!stack.isEmpty()) {
            stacks.add(stack.copy());
         }
      }
      return stacks;
   }

   private void clearPayload() {
      for (int i = 0; i < payload.size(); i++) {
         payload.set(i, ItemStack.EMPTY);
      }
   }

   private void notifyOwner(String line) {
      if (!(level() instanceof ServerLevel serverLevel) || owner == null) {
         return;
      }
      ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
      if (player != null) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // " + line));
      }
   }

   private void recordDeliveryMission() {
      if (!(level() instanceof ServerLevel serverLevel) || owner == null) {
         return;
      }
      ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
      if (player != null) {
         LogisticsMissionHooks.recordCourierDelivery(player, presetId.toString());
      }
   }

   private String statusLine() {
      if (returning && delivered) {
         return "delivered";
      }
      return status == null || status.isBlank() ? "in_transit" : status;
   }

   private String dialogStatusLine() {
      String cargo = presetId == null ? "delivery" : presetId.getPath().replace('_', ' ');
      if (returning && delivered) {
         return "Delivered " + cargo + "; returning to dock.";
      }
      return switch (statusLine()) {
         case "in_transit" -> "Delivering " + cargo + ".";
         case "recovering" -> "Recovering payload to dock.";
         case "delivered" -> "Delivery complete; returning.";
         case "complete" -> "Delivery complete.";
         case "empty_payload" -> "No payload loaded.";
         default -> "Courier " + statusLine().replace('_', ' ') + ".";
      };
   }

   private void syncDialogStatus() {
      setDialogStatus(dialogStatusLine());
   }

   private void setDialogStatus(String value) {
      String clean = cleanStatus(value);
      if (!clean.equals(entityData.get(DATA_DIALOG_STATUS))) {
         entityData.set(DATA_DIALOG_STATUS, clean);
      }
   }

   private static String cleanStatus(String value) {
      return value == null || value.isBlank() ? "Courier idle." : value.replace('_', ' ').trim();
   }

   private String shortJob() {
      return jobId.toString().substring(0, 8);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      ContainerHelper.saveAllItems(output, payload);
      output.putString("job_id", jobId.toString());
      if (owner != null) {
         output.putString("owner", owner.toString());
      }
      output.putString("network_id", networkId());
      output.putInt("source_x", sourceDock.getX());
      output.putInt("source_y", sourceDock.getY());
      output.putInt("source_z", sourceDock.getZ());
      output.putInt("target_x", targetPos.getX());
      output.putInt("target_y", targetPos.getY());
      output.putInt("target_z", targetPos.getZ());
      output.putString("preset", presetId.toString());
      output.putInt("delivery_ticks", deliveryTicks);
      output.putInt("age", age);
      output.putLong("created_tick", createdTick);
      output.putLong("eta_tick", etaTick);
      output.putString("status", statusLine());
      output.putBoolean("returning", returning);
      output.putBoolean("delivered", delivered);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      ContainerHelper.loadAllItems(input, payload);
      jobId = readUuid(input.getStringOr("job_id", "")).orElseGet(UUID::randomUUID);
      owner = readUuid(input.getStringOr("owner", "")).orElse(null);
      networkId = input.getStringOr("network_id", "global");
      sourceDock = new BlockPos(input.getIntOr("source_x", 0), input.getIntOr("source_y", 0), input.getIntOr("source_z", 0));
      targetPos = new BlockPos(input.getIntOr("target_x", 0), input.getIntOr("target_y", 0), input.getIntOr("target_z", 0));
      presetId = Identifier.parse(input.getStringOr("preset", "echologisticsnetwork:unknown"));
      deliveryTicks = input.getIntOr("delivery_ticks", 160);
      age = input.getIntOr("age", 0);
      createdTick = input.getLongOr("created_tick", 0L);
      etaTick = input.getLongOr("eta_tick", createdTick + deliveryTicks);
      status = input.getStringOr("status", "in_transit");
      returning = input.getBooleanOr("returning", false);
      delivered = input.getBooleanOr("delivered", false);
      setDialogStatus(dialogStatusLine());
   }

   private static Optional<UUID> readUuid(String value) {
      if (value == null || value.isBlank()) {
         return Optional.empty();
      }
      try {
         return Optional.of(UUID.fromString(value));
      } catch (IllegalArgumentException exception) {
         return Optional.empty();
      }
   }
}
