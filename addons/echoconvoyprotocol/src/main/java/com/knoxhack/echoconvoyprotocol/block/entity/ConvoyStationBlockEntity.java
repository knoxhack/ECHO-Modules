package com.knoxhack.echoconvoyprotocol.block.entity;

import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock;
import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock.ConvoyBlockKind;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyLogisticsIntegration;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyMissionHooks;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyStationMenu;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyUpgradeMenu;
import com.knoxhack.echoconvoyprotocol.recipe.ConvoyStationRecipe;
import com.knoxhack.echoconvoyprotocol.registry.ModBlockEntities;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.knoxhack.echoconvoyprotocol.registry.ModRecipes;
import com.knoxhack.echoconvoyprotocol.world.ConvoyRouteMarkerIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class ConvoyStationBlockEntity extends BaseContainerBlockEntity {
   public static final int INPUT_SLOT = 0;
   public static final int OUTPUT_SLOT = 1;
   public static final int STORAGE_START = 2;
   public static final int SLOT_COUNT = 11;
   public static final int DATA_PROGRESS = 0;
   public static final int DATA_MAX_PROGRESS = 1;
   public static final int DATA_ENERGY = 2;
   public static final int DATA_MAX_ENERGY = 3;
   public static final int DATA_KIND = 4;
   public static final int DATA_STATUS = 5;
   public static final int DATA_NEARBY_VEHICLES = 6;
   public static final int DATA_COUNT = 7;
   public static final int MAX_ENERGY = 200;

   private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
   private int progress;
   private int maxProgress;
   private int energy = 80;
   private int nearbyVehicles;
   private StationStatus status = StationStatus.IDLE;
   private String lastAction = "Idle";
   @Nullable
   private UUID linkedOwner;
   private String markerRouteId = "";
   private String markerLegId = "";
   @Nullable
   private Identifier roadsideStructureId;

   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         return switch (index) {
            case DATA_PROGRESS -> progress;
            case DATA_MAX_PROGRESS -> maxProgress;
            case DATA_ENERGY -> energy;
            case DATA_MAX_ENERGY -> MAX_ENERGY;
            case DATA_KIND -> kind().ordinal();
            case DATA_STATUS -> status.ordinal();
            case DATA_NEARBY_VEHICLES -> nearbyVehicles;
            default -> 0;
         };
      }

      @Override
      public void set(int index, int value) {
         switch (index) {
            case DATA_PROGRESS -> progress = value;
            case DATA_MAX_PROGRESS -> maxProgress = value;
            case DATA_ENERGY -> energy = value;
            case DATA_STATUS -> status = StationStatus.byId(value);
            case DATA_NEARBY_VEHICLES -> nearbyVehicles = value;
            default -> {
            }
         }
      }

      @Override
      public int getCount() {
         return DATA_COUNT;
      }
   };

   public ConvoyStationBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.CONVOY_STATION.get(), pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ConvoyStationBlockEntity station) {
      if (level.isClientSide()) {
         return;
      }
      station.regenerateEnergy(level);
      station.scanVehicles(level, pos);
      station.tickVehicleSupport(level, pos);
      if (station.kind().recipeDriven()) {
         station.tickRecipe(level);
      } else if (station.kind() != ConvoyBlockKind.VEHICLE_DOCK && station.kind() != ConvoyBlockKind.CARGO_ANCHOR) {
         station.resetProcessing(StationStatus.LINK_READY);
      }
   }

   private void tickRecipe(Level level) {
      if (!(level instanceof ServerLevel serverLevel)) {
         return;
      }
      if (!hasRecipeInput()) {
         resetProcessing(StationStatus.IDLE);
         return;
      }
      RecipeHolder<ConvoyStationRecipe> holder = findRecipe(serverLevel, kind(), recipeInputs());
      if (holder == null) {
         resetProcessing(StationStatus.BAD_INPUT);
         return;
      }
      ConvoyStationRecipe recipe = holder.value();
      ItemStack output = recipe.result().copy();
      if (!canOutput(output)) {
         resetProcessing(StationStatus.OUTPUT_BLOCKED);
         return;
      }
      int energyCost = Math.max(1, recipe.energyCost());
      if (energy < energyCost) {
         maxProgress = Math.max(20, recipe.duration());
         status = StationStatus.CHARGING;
         setChanged();
         return;
      }
      maxProgress = Math.max(20, recipe.duration());
      status = StationStatus.PROCESSING;
      progress++;
      if (progress >= maxProgress) {
         completeRecipe(recipe);
      }
      setChanged();
   }

   @SuppressWarnings("unchecked")
   private static RecipeHolder<ConvoyStationRecipe> findRecipe(ServerLevel level, ConvoyBlockKind kind, List<ItemStack> inputs) {
      return level.getServer().getRecipeManager().getRecipes().stream()
         .filter(holder -> holder.value().getType() == ModRecipes.CONVOY_STATION_PROCESSING_TYPE.get())
         .map(holder -> (RecipeHolder<ConvoyStationRecipe>)holder)
         .filter(holder -> holder.value().matches(kind, inputs, level))
         .findFirst()
         .orElse(null);
   }

   private void tickVehicleSupport(Level level, BlockPos pos) {
      ConvoyBlockKind kind = kind();
      if (kind != ConvoyBlockKind.VEHICLE_DOCK && kind != ConvoyBlockKind.FIELD_REPAIR_STATION && kind != ConvoyBlockKind.CARGO_ANCHOR) {
         return;
      }
      if (level.getGameTime() % 20L != 0L) {
         return;
      }
      ConvoyVehicleEntity vehicle = nearestSupportVehicle(level, pos);
      if (vehicle == null) {
         status = hasOwnerLockedVehicle(level, pos) ? StationStatus.OWNER_LOCKED : StationStatus.NO_VEHICLE;
         return;
      }
      vehicle.setDocked(kind == ConvoyBlockKind.VEHICLE_DOCK);
      ItemStack input = items.get(INPUT_SLOT);
      if (input.isEmpty()) {
         status = StationStatus.LINK_READY;
         lastAction = "Linked " + vehicle.callsign();
         return;
      }
      if (input.is(ModItems.FUEL_CANISTER.get()) && kind == ConvoyBlockKind.VEHICLE_DOCK) {
         vehicle.refuel(60);
         input.shrink(1);
         markSupport("Refueled " + vehicle.callsign());
         ConvoyMissionHooks.recordRefuelRepairNear(level, worldPosition, "vehicle_dock_refuel");
      } else if (input.is(ModItems.BATTERY_CELL.get()) && kind == ConvoyBlockKind.VEHICLE_DOCK) {
         vehicle.recharge(60);
         input.shrink(1);
         markSupport("Recharged " + vehicle.callsign());
      } else if (input.is(ModItems.CONVOY_REPAIR_KIT.get()) && (kind == ConvoyBlockKind.FIELD_REPAIR_STATION || kind == ConvoyBlockKind.VEHICLE_DOCK)) {
         vehicle.repair(45);
         input.shrink(1);
         markSupport("Repaired " + vehicle.callsign());
         ConvoyMissionHooks.recordRefuelRepairNear(level, worldPosition, "vehicle_repair_station");
      } else if (kind == ConvoyBlockKind.CARGO_ANCHOR) {
         if (input.is(ModItems.CARGO_NET.get())) {
            if (ConvoyLogisticsIntegration.depositVehicleCargo(level, worldPosition, vehicle)) {
               input.shrink(1);
               if (input.isEmpty()) {
                  items.set(INPUT_SLOT, ItemStack.EMPTY);
               }
               markSupport("Deposited vehicle cargo into Logistics Network");
               return;
            }
            if (unloadVehicleCargo(vehicle)) {
               input.shrink(1);
               if (input.isEmpty()) {
                  items.set(INPUT_SLOT, ItemStack.EMPTY);
               }
               markSupport("Unloaded vehicle cargo into Cargo Anchor storage");
            } else {
               status = StationStatus.OUTPUT_BLOCKED;
               lastAction = "No cargo moved";
               setChanged();
            }
            return;
         }
         ItemStack moved = input.copyWithCount(1);
         if (vehicle.insertCargo(moved).isEmpty()) {
            input.shrink(1);
            markSupport("Transferred cargo to " + vehicle.callsign());
         } else {
            status = StationStatus.OUTPUT_BLOCKED;
         }
      }
      if (input.isEmpty()) {
         items.set(INPUT_SLOT, ItemStack.EMPTY);
      }
   }

   private void markSupport(String action) {
      status = StationStatus.COMPLETE;
      lastAction = action;
      setChanged();
   }

   private boolean unloadVehicleCargo(ConvoyVehicleEntity vehicle) {
      if (vehicle.cargoStacks().isEmpty()) {
         return false;
      }
      boolean movedAny = false;
      for (int i = 0; i < vehicle.cargoSlots(); i++) {
         ItemStack cargo = vehicle.removeFirstCargo();
         if (cargo.isEmpty()) {
            break;
         }
         int before = cargo.getCount();
         ItemStack remainder = insertStationStorage(cargo);
         if (remainder.getCount() < before) {
            movedAny = true;
         }
         if (!remainder.isEmpty()) {
            vehicle.insertCargo(remainder);
            break;
         }
      }
      return movedAny;
   }

   private void scanVehicles(Level level, BlockPos pos) {
      if (level.getGameTime() % 20L != 0L) {
         return;
      }
      nearbyVehicles = level.getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(pos).inflate(5.0D)).size();
   }

   @Nullable
   public static ConvoyVehicleEntity nearestVehicle(Level level, BlockPos pos) {
      List<ConvoyVehicleEntity> vehicles = level.getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(pos).inflate(5.0D));
      return vehicles.stream()
         .min((left, right) -> Double.compare(left.distanceToSqr(pos.getCenter()), right.distanceToSqr(pos.getCenter())))
         .orElse(null);
   }

   @Nullable
   public static ConvoyVehicleEntity nearestVehicle(Level level, BlockPos pos, Player player) {
      List<ConvoyVehicleEntity> vehicles = level.getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(pos).inflate(5.0D));
      return vehicles.stream()
         .filter(vehicle -> vehicle.isOwner(player))
         .min((left, right) -> Double.compare(left.distanceToSqr(pos.getCenter()), right.distanceToSqr(pos.getCenter())))
         .orElse(null);
   }

   @Nullable
   public ConvoyVehicleEntity nearestUpgradeVehicle(Player player) {
      if (level == null || player == null) {
         return null;
      }
      return nearestVehicle(level, worldPosition, player);
   }

   @Nullable
   private ConvoyVehicleEntity nearestSupportVehicle(Level level, BlockPos pos) {
      List<ConvoyVehicleEntity> vehicles = level.getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(pos).inflate(5.0D));
      return vehicles.stream()
         .filter(this::canSupportVehicle)
         .min((left, right) -> Double.compare(left.distanceToSqr(pos.getCenter()), right.distanceToSqr(pos.getCenter())))
         .orElse(null);
   }

   private boolean hasOwnerLockedVehicle(Level level, BlockPos pos) {
      return level.getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(pos).inflate(5.0D)).stream()
         .anyMatch(vehicle -> !canSupportVehicle(vehicle));
   }

   private boolean canSupportVehicle(ConvoyVehicleEntity vehicle) {
      UUID owner = vehicle.ownerId();
      return owner == null || linkedOwner != null && linkedOwner.equals(owner);
   }

   public void linkOwner(Player player) {
      if (player == null) {
         return;
      }
      UUID playerId = player.getUUID();
      if (!playerId.equals(linkedOwner)) {
         linkedOwner = playerId;
         setChanged();
      }
   }

   public boolean acceptsRouteLeg(Identifier routeId, ConvoyRouteDefinition.RouteLeg leg) {
      if (routeId == null || leg == null) {
         return false;
      }
      if (!markerRouteId.isBlank() && !markerRouteId.equals(routeId.toString())) {
         return false;
      }
      if (!markerLegId.isBlank() && !markerLegId.equals(leg.id())) {
         return false;
      }
      return roadsideStructureId == null || roadsideStructureId.equals(leg.roadsideStructure());
   }

   public void setMarkerMetadata(@Nullable Identifier routeId, @Nullable String legId, @Nullable Identifier roadsideStructureId) {
      markerRouteId = routeId == null ? "" : routeId.toString();
      markerLegId = legId == null ? "" : legId.strip();
      this.roadsideStructureId = roadsideStructureId;
      recordPreciseMarker(routeId, markerLegId);
      setChanged();
   }

   public String markerRequirementLabel() {
      String route = markerRouteId.isBlank() ? "any route" : markerRouteId;
      String leg = markerLegId.isBlank() ? "any leg" : markerLegId;
      return route + " / " + leg;
   }

   public boolean handleMenuButton(Player player, int id) {
      linkOwner(player);
      if (id == ConvoyStationMenu.BUTTON_SCAN) {
         scanVehicles(player.level(), worldPosition);
         player.sendSystemMessage(Component.literal("ECHO CONVOY // Nearby vehicles: " + nearbyVehicles + ". " + lastAction + "."));
         return true;
      }
      if (id == ConvoyStationMenu.BUTTON_UNLOAD && player.level() instanceof ServerLevel) {
         ConvoyVehicleEntity vehicle = nearestVehicle(player.level(), worldPosition, player);
         if (vehicle != null) {
            ItemStack stack = vehicle.removeFirstCargo();
            if (!stack.isEmpty()) {
               int before = stack.getCount();
               ItemStack remainder = insertStationStorage(stack);
               if (!remainder.isEmpty()) {
                  ItemStack rejected = vehicle.insertCargo(remainder);
                  if (!rejected.isEmpty()) {
                     if (!player.getInventory().add(rejected.copy())) {
                        player.drop(rejected.copy(), false);
                     }
                  }
               }
               if (remainder.isEmpty()) {
                  player.sendSystemMessage(Component.literal("ECHO CONVOY // Cargo moved from " + vehicle.kind().displayName() + "."));
                  return true;
               }
               if (remainder.getCount() < before) {
                  player.sendSystemMessage(Component.literal("ECHO CONVOY // Cargo partially moved; remaining stack stayed with the vehicle."));
                  return true;
               }
               player.sendSystemMessage(Component.literal("ECHO CONVOY // Cargo Anchor storage is full."));
               return false;
            }
         } else {
            player.sendSystemMessage(Component.literal("ECHO CONVOY // No owned convoy vehicle in anchor range."));
         }
      }
      return false;
   }

   public ItemStack insertStationStorage(ItemStack stack) {
      ItemStack remainder = stack.copy();
      for (int i = STORAGE_START; i < SLOT_COUNT && !remainder.isEmpty(); i++) {
         ItemStack existing = items.get(i);
         if (existing.isEmpty()) {
            int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            items.set(i, remainder.copyWithCount(moved));
            remainder.shrink(moved);
         } else if (ItemStack.isSameItemSameComponents(existing, remainder) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(remainder.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            remainder.shrink(moved);
         }
      }
      setChanged();
      return remainder;
   }

   public ContainerData data() {
      return data;
   }

   public ConvoyBlockKind kind() {
      return getBlockState().getBlock() instanceof ConvoyBlock block ? block.kind() : ConvoyBlockKind.VEHICLE_WORKBENCH;
   }

   @Override
   protected Component getDefaultName() {
      return Component.literal("ECHO " + kind().displayName());
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> replacement) {
      for (int i = 0; i < Math.min(items.size(), replacement.size()); i++) {
         items.set(i, replacement.get(i));
      }
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      if (kind() == ConvoyBlockKind.VEHICLE_UPGRADE_BAY) {
         return new ConvoyUpgradeMenu(containerId, inventory, this);
      }
      return new ConvoyStationMenu(containerId, inventory, this, data);
   }

   @Override
   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(getBlockPos());
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, items);
      progress = input.getIntOr("progress", 0);
      maxProgress = input.getIntOr("max_progress", 0);
      energy = input.getIntOr("energy", 80);
      nearbyVehicles = input.getIntOr("nearby_vehicles", 0);
      status = StationStatus.byId(input.getIntOr("status", StationStatus.IDLE.ordinal()));
      lastAction = input.getStringOr("last_action", "Idle");
      linkedOwner = readUuid(input.getStringOr("linked_owner", ""));
      markerRouteId = input.getStringOr("marker_route", "");
      markerLegId = input.getStringOr("marker_leg", "");
      roadsideStructureId = Identifier.tryParse(input.getStringOr("roadside_structure", ""));
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, items);
      output.putInt("progress", progress);
      output.putInt("max_progress", maxProgress);
      output.putInt("energy", energy);
      output.putInt("nearby_vehicles", nearbyVehicles);
      output.putInt("status", status.ordinal());
      output.putString("last_action", lastAction == null ? "Idle" : lastAction);
      output.putString("linked_owner", linkedOwner == null ? "" : linkedOwner.toString());
      output.putString("marker_route", markerRouteId == null ? "" : markerRouteId);
      output.putString("marker_leg", markerLegId == null ? "" : markerLegId);
      output.putString("roadside_structure", roadsideStructureId == null ? "" : roadsideStructureId.toString());
   }

   private void regenerateEnergy(Level level) {
      if (energy < MAX_ENERGY && level.getGameTime() % 30L == 0L) {
         energy++;
      }
   }

   private void resetProcessing(StationStatus nextStatus) {
      if (progress != 0 || maxProgress != 0 || status != nextStatus) {
         progress = 0;
         maxProgress = 0;
         status = nextStatus;
         setChanged();
      }
   }

   private void recordPreciseMarker(@Nullable Identifier routeId, String legId) {
      if (!(level instanceof ServerLevel serverLevel) || routeId == null || legId == null || legId.isBlank()) {
         return;
      }
      ConvoyContent.route(routeId).ifPresent(route -> {
         int order = 0;
         for (int i = 0; i < route.legs().size(); i++) {
            if (legId.equals(route.leg(i).id())) {
               order = i;
               break;
            }
         }
         ConvoyRouteMarkerIndex.get(serverLevel).record(routeId, legId, order, worldPosition);
      });
   }

   private boolean canOutput(ItemStack output) {
      ItemStack current = items.get(OUTPUT_SLOT);
      return current.isEmpty() || ItemStack.isSameItemSameComponents(current, output)
         && current.getCount() + output.getCount() <= Math.min(current.getMaxStackSize(), getMaxStackSize(current));
   }

   private void completeRecipe(ConvoyStationRecipe recipe) {
      ItemStack output = recipe.result().copy();
      if (!recipe.consumeIngredients(recipeInputs())) {
         resetProcessing(StationStatus.BAD_INPUT);
         return;
      }
      ItemStack current = items.get(OUTPUT_SLOT);
      if (current.isEmpty()) {
         items.set(OUTPUT_SLOT, output);
      } else {
         current.grow(output.getCount());
      }
      energy = Math.max(0, energy - Math.max(1, recipe.energyCost()));
      progress = 0;
      status = StationStatus.COMPLETE;
      lastAction = "Processed " + output.getHoverName().getString();
   }

   private boolean hasRecipeInput() {
      return recipeInputs().stream().anyMatch(stack -> !stack.isEmpty());
   }

   private List<ItemStack> recipeInputs() {
      List<ItemStack> inputs = new ArrayList<>();
      inputs.add(items.get(INPUT_SLOT));
      for (int i = STORAGE_START; i < SLOT_COUNT; i++) {
         inputs.add(items.get(i));
      }
      return inputs;
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

   public enum StationStatus {
      IDLE("Idle"),
      PROCESSING("Processing"),
      CHARGING("Charging"),
      OUTPUT_BLOCKED("Output blocked"),
      BAD_INPUT("Input rejected"),
      COMPLETE("Complete"),
      LINK_READY("Vehicle link ready"),
      NO_VEHICLE("No vehicle nearby"),
      OWNER_LOCKED("Vehicle owner link required");

      private static final StationStatus[] BY_ID = values();
      private final String label;

      StationStatus(String label) {
         this.label = label;
      }

      public String label() {
         return label;
      }

      public static StationStatus byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : IDLE;
      }
   }
}
