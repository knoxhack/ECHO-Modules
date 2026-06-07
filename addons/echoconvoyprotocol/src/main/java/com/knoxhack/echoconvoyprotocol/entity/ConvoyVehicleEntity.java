package com.knoxhack.echoconvoyprotocol.entity;

import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyMissionHooks;
import com.knoxhack.echoconvoyprotocol.item.VehicleUpgradeItem;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.knoxhack.echoconvoyprotocol.upgrade.ConvoyUpgradeSlot;
import com.knoxhack.echoconvoyprotocol.upgrade.VehicleUpgradeStats;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ConvoyVehicleEntity extends VehicleEntity {
   public static final int MAX_CARGO_SLOTS = 54;
   private static final EntityDataAccessor<Integer> DATA_KIND =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_FUEL =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_BATTERY =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_DAMAGE =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_DOCKED =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_SHIELDING =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_CARGO_FILLED =
      SynchedEntityData.defineId(ConvoyVehicleEntity.class, EntityDataSerializers.INT);

   private final ConvoyVehicleKind kind;
   private final NonNullList<ItemStack> cargo = NonNullList.withSize(MAX_CARGO_SLOTS, ItemStack.EMPTY);
   private final NonNullList<ItemStack> upgrades = NonNullList.withSize(ConvoyUpgradeSlot.values().length, ItemStack.EMPTY);
   @Nullable
   private UUID owner;
   private String activeRouteId = "";
   private int lastCollisionDamageTick;

   public ConvoyVehicleEntity(EntityType<? extends ConvoyVehicleEntity> type, Level level, ConvoyVehicleKind kind) {
      super(type, level);
      this.kind = kind;
      this.blocksBuilding = true;
      this.entityData.set(DATA_KIND, kind.ordinal());
      this.entityData.set(DATA_FUEL, kind.maxFuel() / 2);
      this.entityData.set(DATA_BATTERY, kind.maxBattery() / 3);
   }

   public static ConvoyVehicleEntity create(EntityType<? extends ConvoyVehicleEntity> type, Level level, ConvoyVehicleKind kind) {
      return new ConvoyVehicleEntity(type, level, kind);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_KIND, 0);
      builder.define(DATA_FUEL, 0);
      builder.define(DATA_BATTERY, 0);
      builder.define(DATA_DAMAGE, 0);
      builder.define(DATA_DOCKED, false);
      builder.define(DATA_SHIELDING, 0);
      builder.define(DATA_CARGO_FILLED, 0);
   }

   @Override
   public void tick() {
      super.tick();
      if (isRemoved()) {
         return;
      }
      tickMovement();
      if (!level().isClientSide()) {
         maybeDamageFromCollision();
      }
   }

   private void tickMovement() {
      LivingEntity controller = getControllingPassenger();
      Vec3 motion = getDeltaMovement();
      if (controller != null && !isDisabled()) {
         float forward = forwardInput(controller);
         float strafe = strafeInput(controller);
         if (Math.abs(forward) > 0.01F || Math.abs(strafe) > 0.01F) {
            setYRot(getYRot() - strafe * turnRate());
         }
         if (Math.abs(forward) > 0.01F && hasTravelPower()) {
            double cargoPenalty = 1.0D - Math.min(0.35D, filledCargoSlots() / (double) Math.max(1, cargoSlots()) * cargoWeightPenalty() * 0.35D);
            double speed = speed() * cargoPenalty * (forward < 0.0F ? 0.45D : 1.0D);
            double radians = Math.toRadians(getYRot());
            Vec3 acceleration = new Vec3(-Math.sin(radians) * speed * forward, 0.0D, Math.cos(radians) * speed * forward);
            motion = motion.add(acceleration);
            if (!level().isClientSide() && tickCount % 20 == 0) {
               consumeTravelPower();
            }
         }
      }

      if (!isNoGravity()) {
         motion = motion.add(0.0D, -0.08D, 0.0D);
      }
      double friction = onGround() ? 0.72D : 0.96D;
      motion = new Vec3(motion.x * friction, motion.y * 0.98D, motion.z * friction);
      setDeltaMovement(motion);
      move(MoverType.SELF, motion);
      setOldPosAndRot();
   }

   private static float forwardInput(LivingEntity controller) {
      return controller instanceof ServerPlayer player ? serverInputAxis(player.getLastClientInput().forward(), player.getLastClientInput().backward(), controller.zza) : controller.zza;
   }

   private static float strafeInput(LivingEntity controller) {
      return controller instanceof ServerPlayer player ? serverInputAxis(player.getLastClientInput().left(), player.getLastClientInput().right(), controller.xxa) : controller.xxa;
   }

   private static float serverInputAxis(boolean positive, boolean negative, float fallback) {
      if (positive == negative) {
         return fallback;
      }
      return positive ? 1.0F : -1.0F;
   }

   private boolean hasTravelPower() {
      return fuel() > 0 || (maxBattery() > 0 && battery() > 0);
   }

   private void consumeTravelPower() {
      if (fuel() > 0) {
         setFuel(fuel() - 1);
      } else if (battery() > 0) {
         setBattery(battery() - 1);
      }
   }

   private void maybeDamageFromCollision() {
      if (!horizontalCollision || tickCount - lastCollisionDamageTick < 20) {
         return;
      }
      double speed = getDeltaMovement().horizontalDistance();
      if (speed > 0.16D) {
         int impact = Math.max(1, (int)Math.round((speed - 0.12D) * 18.0D * (1.0D - armor())));
         applyVehicleDamage(impact);
         lastCollisionDamageTick = tickCount;
      }
   }

   @Override
   public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
      InteractionResult superResult = super.interact(player, hand, location);
      if (superResult != InteractionResult.PASS) {
         return superResult;
      }
      if (level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      ItemStack stack = player.getItemInHand(hand);
      if (!claimOrValidateOwner(player)) {
         return InteractionResult.CONSUME;
      }

      if (handleMaintenanceItem(player, stack)) {
         return InteractionResult.SUCCESS_SERVER;
      }

      if (player.isShiftKeyDown()) {
         handleCargoInteraction(player, stack);
         return InteractionResult.SUCCESS_SERVER;
      }

      if (player.startRiding(this)) {
         level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.NEUTRAL, 0.55F, 0.8F);
         sendStatus(player, true);
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.CONSUME;
   }

   private boolean claimOrValidateOwner(Player player) {
      if (owner == null) {
         owner = player.getUUID();
         player.sendSystemMessage(Component.literal("ECHO CONVOY // " + kind.displayName() + " ownership linked."));
         return true;
      }
      if (!isOwner(player)) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Vehicle locked to another operator."));
         return false;
      }
      return true;
   }

   private boolean handleMaintenanceItem(Player player, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }
      boolean changed = false;
      if (stack.is(Items.NAME_TAG)) {
         return applyCallsign(player, stack);
      } else if (stack.is(ModBlocks.FIELD_REPAIR_STATION.get().asItem())) {
         return deployFieldStation(player, stack);
      } else if (stack.is(ModItems.FUEL_CANISTER.get()) && fuel() < maxFuel()) {
         setFuel(Math.min(maxFuel(), fuel() + 50));
         ConvoyMissionHooks.recordRefuelRepair(player, "vehicle_refuel");
         changed = true;
      } else if (stack.is(ModItems.BATTERY_CELL.get()) && maxBattery() > 0 && battery() < maxBattery()) {
         setBattery(Math.min(maxBattery(), battery() + 50));
         changed = true;
      } else if (stack.is(ModItems.CONVOY_REPAIR_KIT.get()) && damage() > 0) {
         repair(35);
         ConvoyMissionHooks.recordRefuelRepair(player, "vehicle_repair");
         changed = true;
      } else if (stack.is(ModItems.RADIATION_SHIELDING_PLATE.get())) {
         if (kind.maxShieldingPlates() <= 0) {
            player.sendSystemMessage(Component.literal("ECHO CONVOY // " + callsign() + " has no shielding mounts."));
            return true;
         }
         if (shieldingPlates() >= kind.maxShieldingPlates()) {
            player.sendSystemMessage(Component.literal("ECHO CONVOY // " + callsign() + " shielding is already full."));
            return true;
         }
         setShieldingPlates(shieldingPlates() + 1);
         changed = true;
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Installed shielding plate on " + callsign() + "."));
      } else if (stack.is(ModItems.ROUTE_BEACON.get())) {
         String routeId = ConvoyProgress.get(player).activeRouteId();
         if (routeId.isBlank()) {
            if (scannerRange() > 0) {
               scanRouteOptions(player);
            } else {
               player.sendSystemMessage(Component.literal("ECHO CONVOY // Route Beacon has no active convoy route to pair."));
            }
            return true;
         }
         if (routeId.equals(activeRouteId())) {
            ConvoyProgress.get(player).pairActiveRouteVehicle(getUUID());
            player.sendSystemMessage(Component.literal("ECHO CONVOY // " + callsign() + " is already paired to " + routeId + "."));
            scanActiveRoute(player);
            return true;
         }
         activeRouteId = routeId;
         ConvoyProgress.get(player).pairActiveRouteVehicle(getUUID());
         changed = true;
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Route Beacon paired " + callsign() + " to " + routeId + "."));
         scanActiveRoute(player);
      }
      if (changed) {
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
         sendStatus(player, false);
         return true;
      }
      return false;
   }

   private boolean applyCallsign(Player player, ItemStack stack) {
      Component customName = stack.get(DataComponents.CUSTOM_NAME);
      if (customName == null || customName.getString().isBlank()) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Rename a Name Tag first to assign a callsign."));
         return true;
      }
      setCustomName(customName.copy());
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      player.sendSystemMessage(Component.literal("ECHO CONVOY // Callsign assigned: " + callsign() + "."));
      return true;
   }

   private boolean deployFieldStation(Player player, ItemStack stack) {
      if (!kind.deploysFieldStation()) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // " + callsign() + " cannot deploy field stations."));
         return true;
      }
      if (battery() < 20) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // " + callsign() + " needs battery 20 to deploy a field station."));
         return true;
      }
      BlockPos target = findFieldStationPosition();
      if (target == null) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // No clear ground for field station deployment."));
         return true;
      }
      BlockState stationState = ModBlocks.FIELD_REPAIR_STATION.get().defaultBlockState();
      level().setBlock(target, stationState, 3);
      if (level().getBlockEntity(target) instanceof com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity station) {
         station.linkOwner(player);
      }
      setBattery(battery() - 20);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      level().playSound(null, target, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.75F, 0.8F);
      player.sendSystemMessage(Component.literal("ECHO CONVOY // " + callsign() + " deployed a Field Repair Station."));
      return true;
   }

   @Nullable
   private BlockPos findFieldStationPosition() {
      BlockPos origin = blockPosition();
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         for (int dy = -1; dy <= 1; dy++) {
            BlockPos target = origin.relative(direction).offset(0, dy, 0);
            if (level().isEmptyBlock(target) && level().getBlockState(target.below()).isFaceSturdy(level(), target.below(), Direction.UP)) {
               return target;
            }
         }
      }
      return null;
   }

   private void scanRouteOptions(Player player) {
      List<ConvoyRouteDefinition> compatible = ConvoyContent.routes().stream()
         .filter(route -> route.acceptsVehicle(kind))
         .limit(3)
         .toList();
      if (compatible.isEmpty()) {
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Scanner found no compatible convoy routes for " + callsign() + "."));
         return;
      }
      player.sendSystemMessage(Component.literal(
         "ECHO CONVOY // " + callsign() + " scanner sweep range " + scannerRange() + "m. Compatible routes:"
      ));
      for (ConvoyRouteDefinition route : compatible) {
         ConvoyRouteService.RouteCheck check = ConvoyRouteService.readiness(player, this, route);
         player.sendSystemMessage(Component.literal(
            "ECHO CONVOY // - " + route.title()
               + " | " + (check.ready() ? "ready" : "blocked: " + check.message())
               + " | threat " + route.threat().label()
               + " | " + route.destinationHint()
         ));
      }
   }

   private void scanActiveRoute(Player player) {
      if (scannerRange() <= 0) {
         return;
      }
      Identifier routeId = Identifier.tryParse(activeRouteId());
      if (routeId == null) {
         return;
      }
      ConvoyContent.route(routeId).ifPresent(route -> {
         ConvoyProgress progress = ConvoyProgress.get(player);
         if (progress.completed(route.id())) {
            player.sendSystemMessage(Component.literal("ECHO CONVOY // Scanner confirms " + route.title() + " is complete."));
            return;
         }
         ConvoyRouteDefinition.RouteLeg leg = route.leg(progress.activeRouteLeg());
         int remaining = remainingDistanceToLeg(progress, leg);
         String range = remaining <= scannerRange() ? "within scanner range" : "beyond scanner range";
         player.sendSystemMessage(Component.literal(
            "ECHO CONVOY // Scanner next leg: " + leg.title()
               + " | " + remaining + "m minimum remaining, " + range
               + " | marker " + leg.roadsideStructure()
               + (leg.requiresCheckpoint() ? " | checkpoint " + route.checkpoint().label() : "")
               + " | threat " + route.threat().label()
         ));
      });
   }

   private int remainingDistanceToLeg(ConvoyProgress progress, ConvoyRouteDefinition.RouteLeg leg) {
      int required = leg.minDistanceFromStart();
      if (required <= 0) {
         return 0;
      }
      return progress.activeRouteStart()
         .map(start -> Math.max(0, required - horizontalDistance(start, blockPosition())))
         .orElse(required);
   }

   private static int horizontalDistance(BlockPos first, BlockPos second) {
      long dx = (long)first.getX() - second.getX();
      long dz = (long)first.getZ() - second.getZ();
      return (int)Math.floor(Math.sqrt(dx * dx + dz * dz));
   }

   private void handleCargoInteraction(Player player, ItemStack stack) {
      if (!stack.isEmpty() && !stack.is(ModItems.CARGO_NET.get())) {
         ItemStack moved = stack.copyWithCount(1);
         if (insertCargo(moved).isEmpty()) {
            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }
            player.sendSystemMessage(Component.literal("ECHO CONVOY // Cargo loaded into " + callsign() + "."));
         } else {
            player.sendSystemMessage(Component.literal("ECHO CONVOY // Cargo bay full."));
         }
         return;
      }
      ItemStack removed = removeFirstCargo();
      if (!removed.isEmpty()) {
         if (!player.getInventory().add(removed.copy())) {
            spawnAtLocation((ServerLevel)level(), removed.copy());
         }
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Cargo recovered from bay."));
      } else {
         sendStatus(player, false);
      }
   }

   public ItemStack insertCargo(ItemStack stack) {
      ItemStack remainder = stack.copy();
      for (int i = 0; i < cargoSlots() && !remainder.isEmpty(); i++) {
         ItemStack existing = cargo.get(i);
         if (existing.isEmpty()) {
            int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            cargo.set(i, remainder.copyWithCount(moved));
            remainder.shrink(moved);
         } else if (ItemStack.isSameItemSameComponents(existing, remainder) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(remainder.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            remainder.shrink(moved);
         }
      }
      syncCargoCount();
      return remainder;
   }

   public ItemStack removeFirstCargo() {
      for (int i = 0; i < cargoSlots(); i++) {
         ItemStack stack = cargo.get(i);
         if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            cargo.set(i, ItemStack.EMPTY);
            syncCargoCount();
            return copy;
         }
      }
      return ItemStack.EMPTY;
   }

   public List<ItemStack> cargoStacks() {
      List<ItemStack> stacks = new ArrayList<>();
      for (int i = 0; i < cargoSlots(); i++) {
         if (!cargo.get(i).isEmpty()) {
            stacks.add(cargo.get(i).copy());
         }
      }
      return stacks;
   }

   public int cargoItemCount(Item item) {
      int total = 0;
      for (int i = 0; i < cargoSlots(); i++) {
         if (cargo.get(i).is(item)) {
            total += cargo.get(i).getCount();
         }
      }
      return total;
   }

   public boolean consumeCargo(Item item, int count) {
      if (cargoItemCount(item) < count) {
         return false;
      }
      int remaining = count;
      for (int i = 0; i < cargoSlots() && remaining > 0; i++) {
         ItemStack stack = cargo.get(i);
         if (stack.is(item)) {
            int moved = Math.min(remaining, stack.getCount());
            stack.shrink(moved);
            remaining -= moved;
            if (stack.isEmpty()) {
               cargo.set(i, ItemStack.EMPTY);
            }
         }
      }
      syncCargoCount();
      return true;
   }

   public void clearCargo() {
      for (int i = 0; i < cargo.size(); i++) {
         cargo.set(i, ItemStack.EMPTY);
      }
      syncCargoCount();
   }

   public void repair(int amount) {
      setVehicleDamage(Math.max(0, damage() - Math.max(0, amount)));
   }

   public void applyVehicleDamage(int amount) {
      if (amount <= 0) {
         return;
      }
      int next = Math.min(maxDamage(), damage() + amount);
      setVehicleDamage(next);
      if (next >= maxDamage() && level() instanceof ServerLevel serverLevel) {
         destroy(serverLevel, getDropItem());
      }
   }

   public void applyHazardDamage(int amount) {
      if (amount <= 0) {
         return;
      }
      int mitigated = Math.max(0, (int)Math.round(amount * (1.0D - hazardDamageReduction())));
      applyVehicleDamage(mitigated);
   }

   public void refuel(int amount) {
      setFuel(Math.min(maxFuel(), fuel() + Math.max(0, amount)));
   }

   public void recharge(int amount) {
      setBattery(Math.min(maxBattery(), battery() + Math.max(0, amount)));
   }

   public int filledCargoSlots() {
      return entityData.get(DATA_CARGO_FILLED);
   }

   private int calculateFilledCargoSlots() {
      int count = 0;
      for (int i = 0; i < cargoSlots(); i++) {
         if (!cargo.get(i).isEmpty()) {
            count++;
         }
      }
      return count;
   }

   private void syncCargoCount() {
      entityData.set(DATA_CARGO_FILLED, calculateFilledCargoSlots());
   }

   public boolean isOwner(Player player) {
      return owner == null || player != null && player.getUUID().equals(owner);
   }

   public @Nullable UUID ownerId() {
      return owner;
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      if (isInvulnerableToBase(source)) {
         return false;
      }
      if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
         discard();
      } else {
         applyVehicleDamage(Math.max(1, Math.round(amount)));
      }
      return true;
   }

   @Override
   public boolean canBeCollidedWith(@Nullable Entity other) {
      return !isRemoved();
   }

   @Override
   public boolean canCollideWith(Entity entity) {
      return entity.canBeCollidedWith(this) && !isPassengerOfSameVehicle(entity);
   }

   @Override
   public boolean isPickable() {
      return !isRemoved();
   }

   @Override
   public boolean isPushable() {
      return true;
   }

   @Override
   protected boolean canAddPassenger(Entity passenger) {
      return getPassengers().isEmpty();
   }

   @Override
   @Nullable
   public LivingEntity getControllingPassenger() {
      Entity passenger = getFirstPassenger();
      return passenger instanceof LivingEntity living ? living : null;
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
      return new Vec3(0.0D, kind.passengerHeight(), 0.0D);
   }

   @Override
   public boolean shouldRiderSit() {
      return true;
   }

   @Override
   public boolean canRiderInteract() {
      return true;
   }

   @Override
   protected Item getDropItem() {
      return switch (kind) {
         case SCRAP_BIKE -> ModItems.SCRAP_BIKE_KIT.get();
         case WASTELAND_ROVER -> ModItems.WASTELAND_ROVER_KIT.get();
         case CARGO_CRAWLER -> ModItems.CARGO_CRAWLER_KIT.get();
         case ARMORED_RELAY_TRUCK -> ModItems.ARMORED_RELAY_TRUCK_KIT.get();
      };
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(getDropItem());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      output.putString("kind", kind.getSerializedName());
      if (owner != null) {
         output.putString("owner", owner.toString());
      }
      output.putInt("fuel", fuel());
      output.putInt("battery", battery());
      output.putInt("vehicle_damage", damage());
      output.putInt("shielding_plates", shieldingPlates());
      output.putBoolean("docked", docked());
      output.putString("active_route", activeRouteId == null ? "" : activeRouteId);
      if (getCustomName() != null) {
         output.putString("callsign", getCustomName().getString());
      }
      ContainerHelper.saveAllItems(output.child("vehicle_upgrades"), upgrades);
      ContainerHelper.saveAllItems(output, cargo);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      owner = readUuid(input.getStringOr("owner", ""));
      input.child("vehicle_upgrades").ifPresent(child -> ContainerHelper.loadAllItems(child, upgrades));
      normalizeUpgradeSlots();
      setFuel(Math.min(maxFuel(), input.getIntOr("fuel", kind.maxFuel() / 2)));
      setBattery(Math.min(maxBattery(), input.getIntOr("battery", kind.maxBattery() / 3)));
      setVehicleDamage(Math.min(maxDamage(), input.getIntOr("vehicle_damage", 0)));
      setShieldingPlates(input.getIntOr("shielding_plates", 0));
      setDocked(input.getBooleanOr("docked", false));
      activeRouteId = input.getStringOr("active_route", "");
      String callsign = input.getStringOr("callsign", "");
      if (!callsign.isBlank()) {
         setCustomName(Component.literal(callsign));
      }
      ContainerHelper.loadAllItems(input, cargo);
      syncCargoCount();
   }

   public ConvoyVehicleKind kind() {
      return kind;
   }

   public int maxFuel() {
      return kind.maxFuel();
   }

   public int maxBattery() {
      return Math.max(0, kind.maxBattery() + upgradeStats().maxBatteryBonus());
   }

   public int maxDamage() {
      return Math.max(1, kind.maxDamage() + upgradeStats().maxDamageBonus());
   }

   public int cargoSlots() {
      return Math.max(0, Math.min(MAX_CARGO_SLOTS, kind.cargoSlots() + upgradeStats().cargoSlotsBonus()));
   }

   public double speed() {
      return kind.speed() * (1.0D + upgradeStats().speedMultiplier());
   }

   public float turnRate() {
      return kind.turnRate() + upgradeStats().turnBonus();
   }

   public double cargoWeightPenalty() {
      return Math.max(0.0D, kind.cargoWeightPenalty() * (1.0D - Math.min(0.9D, upgradeStats().cargoPenaltyReduction())));
   }

   public double armor() {
      return kind.armor() + upgradeStats().armorBonus();
   }

   public int scannerRange() {
      return Math.max(0, kind.scannerRange() + upgradeStats().scannerRangeBonus());
   }

   public ItemStack upgradeStack(ConvoyUpgradeSlot slot) {
      return upgrades.get(slot.ordinal()).copy();
   }

   public boolean canInstallUpgrade(ConvoyUpgradeSlot slot, ItemStack stack) {
      if (slot == null || stack.isEmpty() || stack.getCount() != 1 || !upgrades.get(slot.ordinal()).isEmpty()) {
         return false;
      }
      if (!(stack.getItem() instanceof VehicleUpgradeItem upgrade)) {
         return false;
      }
      return upgrade.targetKind() == kind && upgrade.slot() == slot && !hasUpgrade(stack.getItem());
   }

   public boolean canRemoveUpgrade(ConvoyUpgradeSlot slot) {
      if (slot == null || upgrades.get(slot.ordinal()).isEmpty()) {
         return true;
      }
      VehicleUpgradeStats remainingStats = upgradeStatsExcluding(slot);
      int remainingCargoSlots = effectiveCargoSlots(remainingStats);
      int remainingMaxDamage = effectiveMaxDamage(remainingStats);
      return !hasCargoBeyond(remainingCargoSlots) && damage() <= remainingMaxDamage;
   }

   public boolean setUpgrade(ConvoyUpgradeSlot slot, ItemStack stack) {
      if (slot == null) {
         return false;
      }
      if (stack.isEmpty()) {
         if (!canRemoveUpgrade(slot)) {
            return false;
         }
         upgrades.set(slot.ordinal(), ItemStack.EMPTY);
         clampStatsAfterUpgradeChange();
         return true;
      }
      if (!canInstallUpgrade(slot, stack)) {
         return false;
      }
      upgrades.set(slot.ordinal(), stack.copyWithCount(1));
      clampStatsAfterUpgradeChange();
      return true;
   }

   public ItemStack removeUpgrade(ConvoyUpgradeSlot slot) {
      if (!canRemoveUpgrade(slot)) {
         return ItemStack.EMPTY;
      }
      ItemStack removed = upgradeStack(slot);
      upgrades.set(slot.ordinal(), ItemStack.EMPTY);
      clampStatsAfterUpgradeChange();
      return removed;
   }

   public int fuel() {
      return entityData.get(DATA_FUEL);
   }

   public int battery() {
      return entityData.get(DATA_BATTERY);
   }

   public int damage() {
      return entityData.get(DATA_DAMAGE);
   }

   public int shieldingPlates() {
      return entityData.get(DATA_SHIELDING);
   }

   public double hazardDamageReduction() {
      return Math.min(0.8D, armor() + shieldingPlates() * 0.12D);
   }

   public boolean docked() {
      return entityData.get(DATA_DOCKED);
   }

   public String activeRouteId() {
      return activeRouteId == null ? "" : activeRouteId;
   }

   public void setActiveRouteId(String activeRouteId) {
      this.activeRouteId = activeRouteId == null ? "" : activeRouteId;
   }

   public void setDocked(boolean docked) {
      entityData.set(DATA_DOCKED, docked);
   }

   private void setFuel(int fuel) {
      entityData.set(DATA_FUEL, Math.max(0, Math.min(maxFuel(), fuel)));
   }

   private void setBattery(int battery) {
      entityData.set(DATA_BATTERY, Math.max(0, Math.min(maxBattery(), battery)));
   }

   private void setVehicleDamage(int damage) {
      entityData.set(DATA_DAMAGE, Math.max(0, Math.min(maxDamage(), damage)));
   }

   private void setShieldingPlates(int plates) {
      entityData.set(DATA_SHIELDING, Math.max(0, Math.min(kind.maxShieldingPlates(), plates)));
   }

   private boolean isDisabled() {
      return damage() >= maxDamage();
   }

   private void sendStatus(Player player, boolean actionBar) {
      Component status = Component.literal("ECHO CONVOY // " + callsign()
         + " fuel " + fuel() + "/" + maxFuel()
         + " | battery " + battery() + "/" + maxBattery()
         + " | damage " + damage() + "/" + maxDamage()
         + " | cargo " + filledCargoSlots() + "/" + cargoSlots()
         + (kind.maxShieldingPlates() <= 0 ? "" : " | shielding " + shieldingPlates() + "/" + kind.maxShieldingPlates())
         + (scannerRange() <= 0 ? "" : " | scanner " + scannerRange() + "m")
         + (activeRouteId().isBlank() ? "" : " | route " + activeRouteId()));
      if (player instanceof ServerPlayer serverPlayer) {
         serverPlayer.sendSystemMessage(status, actionBar);
      } else {
         player.sendSystemMessage(status);
      }
   }

   public String callsign() {
      Component customName = getCustomName();
      return customName == null ? kind.displayName() : customName.getString();
   }

   private boolean hasUpgrade(Item item) {
      for (ItemStack upgrade : upgrades) {
         if (upgrade.is(item)) {
            return true;
         }
      }
      return false;
   }

   private VehicleUpgradeStats upgradeStats() {
      return upgradeStatsExcluding(null);
   }

   private VehicleUpgradeStats upgradeStatsExcluding(@Nullable ConvoyUpgradeSlot excludedSlot) {
      double speedMultiplier = 0.0D;
      float turnBonus = 0.0F;
      double cargoPenaltyReduction = 0.0D;
      int cargoSlotsBonus = 0;
      int maxBatteryBonus = 0;
      int maxDamageBonus = 0;
      int scannerRangeBonus = 0;
      double armorBonus = 0.0D;
      for (ConvoyUpgradeSlot slot : ConvoyUpgradeSlot.values()) {
         if (slot == excludedSlot) {
            continue;
         }
         ItemStack stack = upgrades.get(slot.ordinal());
         if (stack.getItem() instanceof VehicleUpgradeItem upgrade && upgrade.targetKind() == kind && upgrade.slot() == slot) {
            VehicleUpgradeStats stats = upgrade.stats();
            speedMultiplier += stats.speedMultiplier();
            turnBonus += stats.turnBonus();
            cargoPenaltyReduction += stats.cargoPenaltyReduction();
            cargoSlotsBonus += stats.cargoSlotsBonus();
            maxBatteryBonus += stats.maxBatteryBonus();
            maxDamageBonus += stats.maxDamageBonus();
            scannerRangeBonus += stats.scannerRangeBonus();
            armorBonus += stats.armorBonus();
         }
      }
      return new VehicleUpgradeStats(
         speedMultiplier,
         turnBonus,
         cargoPenaltyReduction,
         cargoSlotsBonus,
         maxBatteryBonus,
         maxDamageBonus,
         scannerRangeBonus,
         armorBonus
      );
   }

   private int effectiveCargoSlots(VehicleUpgradeStats stats) {
      return Math.max(0, Math.min(MAX_CARGO_SLOTS, kind.cargoSlots() + stats.cargoSlotsBonus()));
   }

   private int effectiveMaxDamage(VehicleUpgradeStats stats) {
      return Math.max(1, kind.maxDamage() + stats.maxDamageBonus());
   }

   private boolean hasCargoBeyond(int slotLimit) {
      for (int i = Math.max(0, slotLimit); i < cargo.size(); i++) {
         if (!cargo.get(i).isEmpty()) {
            return true;
         }
      }
      return false;
   }

   private void normalizeUpgradeSlots() {
      for (ConvoyUpgradeSlot slot : ConvoyUpgradeSlot.values()) {
         ItemStack stack = upgrades.get(slot.ordinal());
         if (stack.isEmpty()) {
            continue;
         }
         if (!(stack.getItem() instanceof VehicleUpgradeItem upgrade) || upgrade.targetKind() != kind || upgrade.slot() != slot) {
            upgrades.set(slot.ordinal(), ItemStack.EMPTY);
         } else if (stack.getCount() > 1) {
            upgrades.set(slot.ordinal(), stack.copyWithCount(1));
         }
      }
      clampStatsAfterUpgradeChange();
   }

   private void clampStatsAfterUpgradeChange() {
      setFuel(fuel());
      setBattery(battery());
      setVehicleDamage(damage());
      syncCargoCount();
   }

   @Nullable
   private static UUID readUuid(String value) {
      if (value == null || value.isBlank()) {
         return null;
      }
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }
}
