package com.knoxhack.echoconvoyprotocol.network;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock;
import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock.ConvoyBlockKind;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.knoxhack.echocore.api.EchoCoreServices;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public record ConvoyTerminalStatePacket(
   String vehicleTitle,
   String vehicleStatus,
   String vehicleCargo,
   List<String> cargoLines,
   String activeRouteTitle,
   String activeRouteStatus,
   String activeLegStatus,
   String checkpointStatus,
   String assistantStatus,
   List<String> assistantLines,
   List<String> nearbyPoiLines,
   List<String> routeBoardLines,
   List<String> routeBoardRouteIds,
   List<String> routeBoardActions,
   String recommendedStartRouteId,
   String claimRouteId,
   boolean hasActiveRoute,
   long gameTime
) implements CustomPacketPayload {
   private static final int MAX_TEXT = 240;
   private static final int MAX_ID = 160;
   private static final int MAX_LINES = 24;

   public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "terminal_state");
   public static final Type<ConvoyTerminalStatePacket> TYPE = new Type<>(ID);
   public static final StreamCodec<RegistryFriendlyByteBuf, ConvoyTerminalStatePacket> CODEC =
      StreamCodec.of(ConvoyTerminalStatePacket::write, ConvoyTerminalStatePacket::read);

   public ConvoyTerminalStatePacket {
      vehicleTitle = safe(vehicleTitle, "No convoy vehicle linked");
      vehicleStatus = safe(vehicleStatus, "Move near an owned vehicle or pair one with a Route Beacon.");
      vehicleCargo = safe(vehicleCargo, "Cargo unavailable");
      cargoLines = copyLines(cargoLines);
      activeRouteTitle = safe(activeRouteTitle, "No active route");
      activeRouteStatus = safe(activeRouteStatus, "Start a route at a Convoy Beacon or from this terminal.");
      activeLegStatus = safe(activeLegStatus, "No active roadside leg.");
      checkpointStatus = safe(checkpointStatus, "No checkpoint warning.");
      assistantStatus = safe(assistantStatus, "Field Assistant standing by.");
      assistantLines = copyLines(assistantLines);
      nearbyPoiLines = copyLines(nearbyPoiLines);
      routeBoardLines = copyLines(routeBoardLines);
      routeBoardRouteIds = copyLines(routeBoardRouteIds);
      routeBoardActions = copyLines(routeBoardActions);
      recommendedStartRouteId = safe(recommendedStartRouteId, "");
      claimRouteId = safe(claimRouteId, "");
      gameTime = Math.max(0L, gameTime);
   }

   public static ConvoyTerminalStatePacket empty() {
      return new ConvoyTerminalStatePacket(
         "No convoy telemetry",
         "Press SCAN or reopen the tab to refresh field data.",
         "Cargo unavailable",
         List.of(),
         "No active route",
         "Awaiting field sync.",
         "No active roadside leg.",
         "No checkpoint warning.",
         "Field Assistant offline until the next scan.",
         List.of("Press SCAN to refresh Convoy guidance."),
         List.of(),
         List.of(),
         List.of(),
         List.of(),
         "",
         "",
         false,
         0L
      );
   }

   public static ConvoyTerminalStatePacket from(ServerPlayer player) {
      if (player == null) {
         return empty();
      }
      ConvoyProgress progress = ConvoyProgress.get(player);
      ConvoyVehicleEntity vehicle = terminalVehicle(player, progress);
      Optional<ConvoyRouteDefinition> activeRoute = activeRoute(progress);
      List<RouteBoardEntry> routeBoard = routeBoardEntries(player, vehicle, progress);
      String startPayload = routeBoard.stream()
         .filter(entry -> "start".equals(entry.action()))
         .map(RouteBoardEntry::routeId)
         .findFirst()
         .orElse("");
      String claimPayload = routeBoard.stream()
         .filter(entry -> "claim".equals(entry.action()))
         .map(RouteBoardEntry::routeId)
         .findFirst()
         .orElse("");
      return new ConvoyTerminalStatePacket(
         vehicleTitle(vehicle),
         vehicleStatus(vehicle, player),
         vehicleCargo(vehicle),
         cargoLines(vehicle),
         activeRoute.map(ConvoyRouteDefinition::title).orElse("No active route"),
         activeRouteStatus(activeRoute, progress, vehicle),
         activeLegStatus(activeRoute, progress, vehicle),
         checkpointStatus(player, activeRoute, progress),
         assistantStatus(player, vehicle, progress, activeRoute, routeBoard),
         assistantLines(player, vehicle, progress, activeRoute, routeBoard),
         nearbyPoiLines(player),
         routeBoard.stream().map(RouteBoardEntry::line).toList(),
         routeBoard.stream().map(RouteBoardEntry::routeId).toList(),
         routeBoard.stream().map(RouteBoardEntry::action).toList(),
         startPayload,
         claimPayload,
         activeRoute.isPresent(),
         player.level().getGameTime()
      );
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   private static ConvoyVehicleEntity terminalVehicle(ServerPlayer player, ConvoyProgress progress) {
      if (player.level() instanceof ServerLevel serverLevel) {
         Optional<UUID> activeVehicle = progress.activeRouteVehicle();
         if (activeVehicle.isPresent()) {
            Entity entity = serverLevel.getEntity(activeVehicle.get());
            if (entity instanceof ConvoyVehicleEntity vehicle && vehicle.isOwner(player)) {
               return vehicle;
            }
         }
      }
      return player.level().getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(player.blockPosition()).inflate(12.0D, 4.0D, 12.0D)).stream()
         .filter(vehicle -> vehicle.isOwner(player))
         .min(Comparator.comparingDouble(vehicle -> vehicle.distanceToSqr(player)))
         .orElse(null);
   }

   private static Optional<ConvoyRouteDefinition> activeRoute(ConvoyProgress progress) {
      Identifier routeId = Identifier.tryParse(progress.activeRouteId());
      return routeId == null ? Optional.empty() : ConvoyContent.route(routeId);
   }

   private static String vehicleTitle(ConvoyVehicleEntity vehicle) {
      if (vehicle == null) {
         return "No owned vehicle nearby";
      }
      return vehicle.callsign() + " | " + vehicle.kind().displayName();
   }

   private static String vehicleStatus(ConvoyVehicleEntity vehicle, ServerPlayer player) {
      if (vehicle == null) {
         return "No owned vehicle within 12 blocks. Pair an active route vehicle with a Route Beacon.";
      }
      int distance = (int)Math.floor(Math.sqrt(vehicle.distanceToSqr(player)));
      return "Fuel " + vehicle.fuel() + "/" + vehicle.maxFuel()
         + " | Battery " + vehicle.battery() + "/" + vehicle.maxBattery()
         + " | Damage " + vehicle.damage() + "/" + vehicle.maxDamage()
         + " | Shielding " + vehicle.shieldingPlates() + "/" + vehicle.kind().maxShieldingPlates()
         + " | Scanner " + vehicle.scannerRange() + "m"
         + " | " + distance + "m away";
   }

   private static String vehicleCargo(ConvoyVehicleEntity vehicle) {
      if (vehicle == null) {
         return "Cargo unavailable";
      }
      return "Cargo " + vehicle.filledCargoSlots() + "/" + vehicle.cargoSlots() + " slots";
   }

   private static List<String> cargoLines(ConvoyVehicleEntity vehicle) {
      if (vehicle == null) {
         return List.of("No vehicle cargo bay in terminal range.");
      }
      List<String> lines = new ArrayList<>();
      for (ItemStack stack : vehicle.cargoStacks()) {
         lines.add(stack.getCount() + "x " + stack.getHoverName().getString());
         if (lines.size() >= 5) {
            break;
         }
      }
      return lines.isEmpty() ? List.of("Cargo bay empty.") : lines;
   }

   private static String activeRouteStatus(Optional<ConvoyRouteDefinition> route, ConvoyProgress progress, ConvoyVehicleEntity vehicle) {
      if (route.isEmpty()) {
         return "No active convoy contract.";
      }
      ConvoyRouteDefinition definition = route.get();
      return "Leg " + Math.min(progress.activeRouteLeg() + 1, definition.legs().size()) + "/" + definition.legs().size()
         + " | Destination " + definition.destinationHint()
         + " | Threat " + definition.threat().label()
         + (vehicle == null ? " | paired vehicle unavailable" : " | vehicle " + vehicle.callsign());
   }

   private static String activeLegStatus(Optional<ConvoyRouteDefinition> route, ConvoyProgress progress, ConvoyVehicleEntity vehicle) {
      if (route.isEmpty()) {
         return "No active roadside leg.";
      }
      ConvoyRouteDefinition definition = route.get();
      ConvoyRouteDefinition.RouteLeg leg = definition.leg(progress.activeRouteLeg());
      int remaining = remainingDistance(progress, vehicle == null ? null : vehicle.blockPosition(), leg);
      String range = vehicle != null && vehicle.scannerRange() > 0
         ? remaining <= vehicle.scannerRange() ? "within scanner range" : "outside scanner range"
         : "scanner unavailable";
      return leg.title()
         + " | marker " + leg.roadsideStructure()
         + " | remaining " + remaining + "m minimum"
         + " | " + range;
   }

   private static String checkpointStatus(ServerPlayer player, Optional<ConvoyRouteDefinition> route, ConvoyProgress progress) {
      if (route.isEmpty()) {
         return "No checkpoint warning.";
      }
      ConvoyRouteDefinition definition = route.get();
      ConvoyRouteDefinition.RouteLeg leg = definition.leg(progress.activeRouteLeg());
      if (!leg.requiresCheckpoint()) {
         return "Next leg has no faction checkpoint.";
      }
      if (definition.minReputation() <= 0 || progress.checkpointCleared(definition.id())) {
         return definition.checkpoint().label() + " ready.";
      }
      int reputation = EchoCoreServices.factionProfile(player, definition.checkpointFactionId())
         .map(profile -> profile.reputation())
         .orElse(Integer.MIN_VALUE);
      return reputation < definition.minReputation()
         ? definition.checkpoint().blockedMessage(definition, reputation)
         : definition.checkpoint().label() + " ready: reputation " + reputation + "/" + definition.minReputation() + ".";
   }

   private static List<String> nearbyPoiLines(ServerPlayer player) {
      Level level = player.level();
      BlockPos center = player.blockPosition();
      List<PoiLine> pois = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-18, -4, -18), center.offset(18, 4, 18))) {
         BlockPos candidate = pos.immutable();
         if (!(level.getBlockState(candidate).getBlock() instanceof ConvoyBlock block)) {
            continue;
         }
         ConvoyBlockKind kind = block.kind();
         if (kind != ConvoyBlockKind.ROADSIDE_SIGNAL_MARKER
            && kind != ConvoyBlockKind.CARGO_ANCHOR
            && kind != ConvoyBlockKind.VEHICLE_DOCK
            && kind != ConvoyBlockKind.FIELD_REPAIR_STATION
            && kind != ConvoyBlockKind.CONVOY_BEACON) {
            continue;
         }
         int distance = horizontalDistance(center, candidate);
         String detail = kind.displayName() + " " + distance + "m";
         if (level.getBlockEntity(candidate) instanceof ConvoyStationBlockEntity station && kind == ConvoyBlockKind.ROADSIDE_SIGNAL_MARKER) {
            detail += " | " + station.markerRequirementLabel();
         }
         pois.add(new PoiLine(distance, detail));
      }
      return pois.stream()
         .sorted(Comparator.comparingInt(PoiLine::distance))
         .limit(6)
         .map(PoiLine::line)
         .toList();
   }

   private static List<RouteBoardEntry> routeBoardEntries(ServerPlayer player, ConvoyVehicleEntity vehicle, ConvoyProgress progress) {
      List<RouteBoardEntry> entries = new ArrayList<>();
      String activeRouteId = progress.activeRouteId();
      for (ConvoyRouteDefinition route : ConvoyContent.routes()) {
         String action;
         String state;
         if (route.id().toString().equals(activeRouteId)) {
            action = "active";
            state = "ACTIVE";
         } else if (progress.claimed(route.id())) {
            action = "claimed";
            state = "CLAIMED";
         } else if (progress.completed(route.id())) {
            action = "claim";
            state = "REWARD READY";
         } else if (!activeRouteId.isBlank()) {
            action = "blocked";
            state = "active route in progress";
         } else {
            ConvoyRouteService.RouteCheck check = ConvoyRouteService.readiness(player, vehicle, route);
            action = check.ready() ? "start" : "blocked";
            state = check.ready() ? "READY" : check.message();
         }
         entries.add(new RouteBoardEntry(route.id().toString(), action, route.title()
            + " | " + route.requiredVehicle().replace('_', ' ')
            + " | fuel " + ConvoyRouteService.requiredFuel(route)
            + " | " + route.threat().label()
            + " | " + state,
            routeHint(route, action, state, player, vehicle)));
         if (entries.size() >= 7) {
            break;
         }
      }
      return entries;
   }

   private static String routeHint(
      ConvoyRouteDefinition route,
      String action,
      String state,
      ServerPlayer player,
      ConvoyVehicleEntity vehicle
   ) {
      return switch (action) {
         case "start" -> "Ready: select START, use a Convoy Beacon nearby, or launch from Field Assistant.";
         case "claim" -> "Reward ready: claim payout from the Convoy tab. Completed rewards are protected from duplicate claims.";
         case "active" -> "Active: drive to the next route leg, then use SIGNAL beside the matching Roadside Signal Marker.";
         case "claimed" -> "Complete: this route payout has already been claimed.";
         default -> "active route in progress".equals(state)
            ? "Finish or close the active convoy route before starting another."
            : ConvoyRouteService.readinessHint(player, vehicle, route);
      };
   }

   private static String assistantStatus(
      ServerPlayer player,
      ConvoyVehicleEntity vehicle,
      ConvoyProgress progress,
      Optional<ConvoyRouteDefinition> activeRoute,
      List<RouteBoardEntry> routeBoard
   ) {
      Optional<RouteBoardEntry> claim = firstAction(routeBoard, "claim");
      if (claim.isPresent()) {
         return "Reward ready: " + routeTitle(claim.get().routeId());
      }
      if (activeRoute.isPresent()) {
         return vehicle == null
            ? "Active route: paired vehicle telemetry missing"
            : "Active route: " + activeRoute.get().title();
      }
      Optional<RouteBoardEntry> start = firstAction(routeBoard, "start");
      if (start.isPresent()) {
         return "Ready to launch: " + routeTitle(start.get().routeId());
      }
      if (vehicle == null) {
         return "Setup needed: deploy a convoy vehicle";
      }
      Optional<RouteBoardEntry> blocked = firstAction(routeBoard, "blocked");
      return blocked.map(entry -> "Prep needed: " + routeTitle(entry.routeId())).orElse("Routes unavailable");
   }

   private static List<String> assistantLines(
      ServerPlayer player,
      ConvoyVehicleEntity vehicle,
      ConvoyProgress progress,
      Optional<ConvoyRouteDefinition> activeRoute,
      List<RouteBoardEntry> routeBoard
   ) {
      Optional<RouteBoardEntry> claim = firstAction(routeBoard, "claim");
      if (claim.isPresent()) {
         return List.of(claim.get().hint(), "If your inventory is full, rewards drop safely at your feet.");
      }
      if (activeRoute.isPresent()) {
         List<String> lines = new ArrayList<>();
         if (vehicle == null) {
            lines.add("Move near the paired vehicle or use a Route Beacon on it to relink route telemetry.");
         } else {
            ConvoyRouteDefinition.RouteLeg leg = activeRoute.get().leg(progress.activeRouteLeg());
            lines.add("Find " + leg.title() + ", keep the paired vehicle nearby, then press SIGNAL beside the correct marker.");
         }
         String checkpoint = checkpointStatus(player, activeRoute, progress);
         if (checkpoint.toLowerCase(java.util.Locale.ROOT).contains("blocked")
            || checkpoint.toLowerCase(java.util.Locale.ROOT).contains("requires")) {
            lines.add(checkpoint);
         } else {
            lines.add("Roadside Signal Markers only close a leg after its distance and route requirements are met.");
         }
         return lines;
      }
      Optional<RouteBoardEntry> start = firstAction(routeBoard, "start");
      if (start.isPresent()) {
         return List.of(start.get().hint(), "Load extra fuel, repair kits, and route beacons before leaving the beacon.");
      }
      if (vehicle == null) {
         return List.of(
            "Craft a Vehicle Workbench with iron, copper, redstone, and a piston.",
            "Process a Vehicle Frame, two Scrap Tires, and a Fuel Canister into a Scrap Bike Kit.",
            "Deploy the kit on flat ground, then keep the terminal within 12 blocks for route telemetry."
         );
      }
      Optional<RouteBoardEntry> blocked = firstAction(routeBoard, "blocked");
      if (blocked.isPresent()) {
         return List.of(blocked.get().hint(), "Use SCAN after loading cargo or fuel so the route board refreshes.");
      }
      return List.of("No convoy route definitions are loaded. Check data packs, then reload or reopen the world.");
   }

   private static Optional<RouteBoardEntry> firstAction(List<RouteBoardEntry> routeBoard, String action) {
      return routeBoard.stream().filter(entry -> action.equals(entry.action())).findFirst();
   }

   private static String routeTitle(String routeId) {
      Identifier id = Identifier.tryParse(routeId);
      return id == null ? routeId : ConvoyContent.route(id).map(ConvoyRouteDefinition::title).orElse(routeId);
   }

   private static int remainingDistance(ConvoyProgress progress, BlockPos current, ConvoyRouteDefinition.RouteLeg leg) {
      int required = leg.minDistanceFromStart();
      if (required <= 0) {
         return 0;
      }
      Optional<BlockPos> start = progress.activeRouteStart();
      if (start.isEmpty() || current == null) {
         return required;
      }
      return Math.max(0, required - horizontalDistance(start.get(), current));
   }

   private static int horizontalDistance(BlockPos first, BlockPos second) {
      long dx = (long)first.getX() - second.getX();
      long dz = (long)first.getZ() - second.getZ();
      return (int)Math.floor(Math.sqrt(dx * dx + dz * dz));
   }

   private static void write(RegistryFriendlyByteBuf buffer, ConvoyTerminalStatePacket packet) {
      buffer.writeUtf(packet.vehicleTitle(), MAX_TEXT);
      buffer.writeUtf(packet.vehicleStatus(), MAX_TEXT);
      buffer.writeUtf(packet.vehicleCargo(), MAX_TEXT);
      writeLines(buffer, packet.cargoLines());
      buffer.writeUtf(packet.activeRouteTitle(), MAX_TEXT);
      buffer.writeUtf(packet.activeRouteStatus(), MAX_TEXT);
      buffer.writeUtf(packet.activeLegStatus(), MAX_TEXT);
      buffer.writeUtf(packet.checkpointStatus(), MAX_TEXT);
      buffer.writeUtf(packet.assistantStatus(), MAX_TEXT);
      writeLines(buffer, packet.assistantLines());
      writeLines(buffer, packet.nearbyPoiLines());
      writeLines(buffer, packet.routeBoardLines());
      writeLines(buffer, packet.routeBoardRouteIds());
      writeLines(buffer, packet.routeBoardActions());
      buffer.writeUtf(packet.recommendedStartRouteId(), MAX_ID);
      buffer.writeUtf(packet.claimRouteId(), MAX_ID);
      buffer.writeBoolean(packet.hasActiveRoute());
      buffer.writeVarLong(packet.gameTime());
   }

   private static ConvoyTerminalStatePacket read(RegistryFriendlyByteBuf buffer) {
      return new ConvoyTerminalStatePacket(
         buffer.readUtf(MAX_TEXT),
         buffer.readUtf(MAX_TEXT),
         buffer.readUtf(MAX_TEXT),
         readLines(buffer),
         buffer.readUtf(MAX_TEXT),
         buffer.readUtf(MAX_TEXT),
         buffer.readUtf(MAX_TEXT),
         buffer.readUtf(MAX_TEXT),
         buffer.readUtf(MAX_TEXT),
         readLines(buffer),
         readLines(buffer),
         readLines(buffer),
         readLines(buffer),
         readLines(buffer),
         buffer.readUtf(MAX_ID),
         buffer.readUtf(MAX_ID),
         buffer.readBoolean(),
         buffer.readVarLong()
      );
   }

   private static void writeLines(RegistryFriendlyByteBuf buffer, List<String> lines) {
      List<String> safe = copyLines(lines);
      buffer.writeVarInt(safe.size());
      for (String line : safe) {
         buffer.writeUtf(line, MAX_TEXT);
      }
   }

   private static List<String> readLines(RegistryFriendlyByteBuf buffer) {
      int count = Math.max(0, Math.min(MAX_LINES, buffer.readVarInt()));
      List<String> lines = new ArrayList<>();
      for (int i = 0; i < count; i++) {
         lines.add(buffer.readUtf(MAX_TEXT));
      }
      return List.copyOf(lines);
   }

   private static List<String> copyLines(List<String> lines) {
      if (lines == null || lines.isEmpty()) {
         return List.of();
      }
      return lines.stream()
         .filter(line -> line != null && !line.isBlank())
         .map(line -> line.length() > MAX_TEXT ? line.substring(0, MAX_TEXT) : line)
         .limit(MAX_LINES)
         .toList();
   }

   private static String safe(String value, String fallback) {
      return value == null || value.isBlank() ? fallback : value.strip();
   }

   private record PoiLine(int distance, String line) {
   }

   private record RouteBoardEntry(String routeId, String action, String line, String hint) {
   }
}
