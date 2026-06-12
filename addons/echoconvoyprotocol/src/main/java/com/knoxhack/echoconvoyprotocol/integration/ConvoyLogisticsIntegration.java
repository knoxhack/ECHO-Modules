package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntime;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class ConvoyLogisticsIntegration {
   private static final String SERVICE = "com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService";

   private ConvoyLogisticsIntegration() {
   }

   public static void register() {
      if (!available()) {
         return;
      }
      try {
         Class.forName("com.knoxhack.echoconvoyprotocol.integration.ConvoyLogisticsEndpointProvider")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError ignored) {
         // Optional integration: Convoy keeps local crate behavior if Logistics is absent or older.
      }
   }

   public static boolean available() {
      return EchoRuntimeModules.isLoaded("echologisticsnetwork");
   }

   public static boolean depositVehicleCargo(Level level, BlockPos anchorPos, ConvoyVehicleEntity vehicle) {
      if (!available() || vehicle == null) {
         return false;
      }
      List<ItemStack> cargo = vehicle.cargoStacks();
      if (cargo.isEmpty()) {
         return false;
      }
      try {
         Method deliverPayload = service().getMethod("deliverPayload", Level.class, BlockPos.class, List.class);
         Object delivered = deliverPayload.invoke(null, level, anchorPos, cargo);
         if (Boolean.TRUE.equals(delivered)) {
            vehicle.clearCargo();
            return true;
         }
      } catch (ReflectiveOperationException | LinkageError ignored) {
         return false;
      }
      return false;
   }

   public static LogisticsStatus syncInventory(ConvoyMultiblockControllerBlockEntity controller, ConvoyRouteDefinition route) {
      if (controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
         return LogisticsStatus.offline("Controller is not loaded on a server level.");
      }
      String networkId = networkId(controller, route);
      String loadoutId = loadoutId(controller, route);
      LogisticsStatus status = snapshot(level, controller.getBlockPos(), networkId, loadoutId);
      int deliveredCargo = countNearby(controller, ModItems.FIELD_SUPPLY_CRATE.get());
      int deliveredFuel = countNearby(controller, ModItems.FUEL_CELL.get());
      controller.convoyState().updateLogistics(networkId, loadoutId, status.networkOnline(), status.loadoutReady(), status.activeDeliveries());
      if (deliveredCargo > 0 || deliveredFuel > 0) {
         controller.convoyState().markCargoDelivered(deliveredCargo, deliveredFuel);
      }
      return status;
   }

   public static boolean requestRouteSupplies(ConvoyMultiblockControllerBlockEntity controller, ConvoyRouteDefinition route) {
      if (!available() || controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
         if (controller != null) {
            controller.convoyState().setLastDiagnostic("Logistics Network is not loaded; use local crate inputs.");
         }
         return false;
      }
      String loadoutId = loadoutId(controller, route);
      if (loadoutId.isBlank()) {
         controller.convoyState().setLastDiagnostic("Route has no Logistics loadout configured.");
         return false;
      }
      BlockPos target = findCrate(controller, ModBlocks.CARGO_INPUT_CRATE.get(), ModBlocks.FUEL_INPUT_TANK.get())
         .orElse(controller.getBlockPos());
      String networkId = networkId(controller, route);
      try {
         Method request = service().getMethod(
            "requestLoadout",
            ServerLevel.class,
            UUID.class,
            BlockPos.class,
            BlockPos.class,
            String.class,
            String.class
         );
         Object requested = request.invoke(null, level, null, controller.getBlockPos(), target, loadoutId, networkId);
         if (Boolean.TRUE.equals(requested)) {
            controller.convoyState().markLogisticsRequestStarted(networkId, loadoutId);
            return true;
         }
      } catch (ReflectiveOperationException | LinkageError ignored) {
         controller.convoyState().setLastDiagnostic("Logistics request API unavailable; use local crate inputs.");
      }
      return false;
   }

   public static int cancelRouteSupplyRequest(ConvoyMultiblockControllerBlockEntity controller) {
      if (!available() || controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
         return 0;
      }
      try {
         Method cancel = service().getMethod("cancelActiveDeliveries", ServerLevel.class, UUID.class, BlockPos.class, String.class);
         Object cancelled = cancel.invoke(null, level, null, controller.getBlockPos(), controller.convoyState().logisticsNetworkId());
         int count = cancelled instanceof Number number ? number.intValue() : 0;
         if (count > 0) {
            controller.convoyState().markLogisticsRequestCancelled();
         }
         return count;
      } catch (ReflectiveOperationException | LinkageError ignored) {
         return 0;
      }
   }

   public static boolean exportSalvageManifest(ConvoyMultiblockControllerBlockEntity controller, ConvoyRouteDefinition route) {
      if (controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
         return false;
      }
      List<ItemStack> payload = payloadFor(route);
      if (payload.isEmpty()) {
         payload = List.of(new ItemStack(ModItems.VEHICLE_ARMOR_PLATE.get()));
      }
      if (available()) {
         try {
            Method deposit = service().getMethod("depositPayload", Level.class, BlockPos.class, String.class, List.class);
            Object exported = deposit.invoke(null, level, controller.getBlockPos(), controller.convoyState().logisticsNetworkId(), payload);
            if (Boolean.TRUE.equals(exported)) {
               controller.convoyState().markSalvageExported(true);
               ConvoyMissionHooks.recordSalvageExportNear(level, controller.getBlockPos(), route == null ? null : route.id());
               return true;
            }
         } catch (ReflectiveOperationException | LinkageError ignored) {
            // Fall through to output crate fallback.
         }
      }
      boolean inserted = fallbackOutput(controller, payload);
      controller.convoyState().markSalvageExported(false);
      if (inserted) {
         ConvoyMissionHooks.recordSalvageExportNear(level, controller.getBlockPos(), route == null ? null : route.id());
      }
      return inserted;
   }

   public static LogisticsStatus snapshot(Level level, BlockPos origin, String networkId, String loadoutId) {
      if (!available() || level == null || origin == null) {
         return LogisticsStatus.offline("Logistics Network not loaded.");
      }
      try {
         Method snapshot = service().getMethod("snapshot", Level.class, BlockPos.class, String.class, Player.class);
         Object value = snapshot.invoke(null, level, origin, networkId, null);
         boolean dockOnline = (boolean)value.getClass().getMethod("dockOnline").invoke(value);
         int activeDeliveries = ((Number)value.getClass().getMethod("activeDeliveries").invoke(value)).intValue();
         boolean loadoutReady = loadoutReady(value, loadoutId);
         return new LogisticsStatus(true, dockOnline, loadoutReady, activeDeliveries, "snapshot");
      } catch (ReflectiveOperationException | LinkageError exception) {
         return LogisticsStatus.offline("Logistics snapshot unavailable.");
      }
   }

   private static boolean loadoutReady(Object snapshot, String loadoutId) throws ReflectiveOperationException {
      if (loadoutId == null || loadoutId.isBlank()) {
         return (boolean)snapshot.getClass().getMethod("selectedReady").invoke(snapshot);
      }
      Object rows = snapshot.getClass().getMethod("loadoutReadiness").invoke(snapshot);
      if (rows instanceof Iterable<?> iterable) {
         for (Object row : iterable) {
            Object presetId = row.getClass().getMethod("presetId").invoke(row);
            if (loadoutId.equals(String.valueOf(presetId))) {
               return (boolean)row.getClass().getMethod("ready").invoke(row);
            }
         }
      }
      return false;
   }

   private static String networkId(ConvoyMultiblockControllerBlockEntity controller, ConvoyRouteDefinition route) {
      if (route != null && !route.logisticsNetworkId().isBlank()) {
         return route.logisticsNetworkId();
      }
      return controller == null ? "global" : controller.convoyState().logisticsNetworkId();
   }

   private static String loadoutId(ConvoyMultiblockControllerBlockEntity controller, ConvoyRouteDefinition route) {
      if (route != null && route.logisticsLoadoutId() != null) {
         return route.logisticsLoadoutId().toString();
      }
      return controller == null ? "" : controller.convoyState().logisticsLoadoutId();
   }

   private static int countNearby(ConvoyMultiblockControllerBlockEntity controller, net.minecraft.world.item.Item item) {
      int count = 0;
      for (BlockPos pos : candidatePositions(controller)) {
         if (controller.getLevel().getBlockEntity(pos) instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
               ItemStack stack = container.getItem(slot);
               if (stack.is(item)) {
                  count += stack.getCount();
               }
            }
         }
      }
      return count;
   }

   private static java.util.Optional<BlockPos> findCrate(ConvoyMultiblockControllerBlockEntity controller, Block... blocks) {
      for (BlockPos pos : candidatePositions(controller)) {
         Block block = controller.getLevel().getBlockState(pos).getBlock();
         for (Block expected : blocks) {
            if (block == expected) {
               return java.util.Optional.of(pos.immutable());
            }
         }
      }
      return java.util.Optional.empty();
   }

   private static List<BlockPos> candidatePositions(ConvoyMultiblockControllerBlockEntity controller) {
      if (controller == null || controller.getLevel() == null) {
         return List.of();
      }
      List<BlockPos> positions = new ArrayList<>();
      controller.getRuntime().map(MultiblockRuntime::matchedBlocks).ifPresent(positions::addAll);
      if (positions.isEmpty()) {
         for (BlockPos pos : BlockPos.betweenClosed(controller.getBlockPos().offset(-6, -2, -6), controller.getBlockPos().offset(6, 5, 6))) {
            positions.add(pos.immutable());
         }
      }
      return positions;
   }

   private static List<ItemStack> payloadFor(ConvoyRouteDefinition route) {
      if (route == null) {
         return List.of();
      }
      return route.rewards().stream().map(ConvoyRouteDefinition.StackSpec::stack).filter(stack -> !stack.isEmpty()).toList();
   }

   private static boolean fallbackOutput(ConvoyMultiblockControllerBlockEntity controller, List<ItemStack> payload) {
      java.util.Optional<BlockPos> output = findCrate(controller, ModBlocks.CARGO_OUTPUT_CRATE.get(), ModBlocks.FUEL_OUTPUT_TANK.get());
      if (output.isEmpty() || !(controller.getLevel().getBlockEntity(output.get()) instanceof Container container)) {
         return false;
      }
      for (ItemStack stack : payload) {
         ItemStack remaining = stack.copy();
         for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
               int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
               container.setItem(slot, remaining.copyWithCount(moved));
               remaining.shrink(moved);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
               int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
               if (moved > 0) {
                  existing.grow(moved);
                  remaining.shrink(moved);
               }
            }
         }
         if (!remaining.isEmpty()) {
            return false;
         }
      }
      container.setChanged();
      return true;
   }

   private static Class<?> service() throws ClassNotFoundException {
      return Class.forName(SERVICE);
   }

   public record LogisticsStatus(
      boolean available,
      boolean networkOnline,
      boolean loadoutReady,
      int activeDeliveries,
      String detail
   ) {
      public static LogisticsStatus offline(String detail) {
         return new LogisticsStatus(false, false, false, 0, detail == null ? "offline" : detail);
      }
   }
}
