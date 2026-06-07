package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoRouteRecordService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class ConvoyCoreIntegration {
   public static final String CHAPTER_ID = "convoy_protocol";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final EchoRouteRecordService ROUTE_SERVICE = ConvoyCoreIntegration::routeRecords;
   private static final EchoDiagnosticService DIAGNOSTIC_SERVICE = ConvoyCoreIntegration::diagnostics;

   private ConvoyCoreIntegration() {
   }

   public static void registerAddonChapter() {
      if (REGISTERED.compareAndSet(false, true) && !EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
         EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
               return CHAPTER_ID;
            }

            @Override
            public String modId() {
               return EchoConvoyProtocol.MODID;
            }

            @Override
            public String displayName() {
               return "ECHO: Convoy Protocol";
            }

            @Override
            public String summary() {
               return "Vehicles, convoy routes, roadside threats, fuel, cargo, and faction checkpoint travel.";
            }

            @Override
            public boolean isAvailable(Player player) {
               return player != null;
            }

            @Override
            public String statusLine(Player player) {
               if (player == null) {
                  return "CONVOY: waiting for operator telemetry.";
               }
               ConvoyProgress progress = ConvoyProgress.get(player);
               long claimable = ConvoyRouteService.claimableRewards(player);
               return "CONVOY: " + ConvoyContent.routes().size() + " known routes"
                  + (progress.activeRouteId().isBlank() ? "" : ", active " + progress.activeRouteId())
                  + ", rewards " + claimable + ".";
            }
         });
      }
      EchoCoreServices.registerRouteRecordService(ROUTE_SERVICE);
      EchoCoreServices.registerDiagnosticService(DIAGNOSTIC_SERVICE);
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      if (player == null) {
         return List.of();
      }
      ConvoyProgress progress = ConvoyProgress.get(player);
      ConvoyVehicleEntity vehicle = relevantVehicle(player, progress);
      return ConvoyContent.routes().stream()
         .map(route -> {
            boolean complete = progress.completed(route.id());
            String status = routeStatus(player, progress, vehicle, route);
            return new EchoRouteRecord(
               ConvoyTerminalIds.id("route/" + route.id().getPath()),
               CHAPTER_ID,
               route.title(),
               routeCategory(route),
               routeDimensionHint(progress, vehicle, route),
               status,
               routeSummary(player, progress, vehicle, route),
               complete
            );
         })
         .toList();
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      }
      ConvoyProgress progress = ConvoyProgress.get(player);
      List<EchoDiagnosticBlocker> diagnostics = new ArrayList<>();
      ConvoyVehicleEntity vehicle = relevantVehicle(player, progress);
      Optional<ConvoyRouteDefinition> activeRoute = activeRoute(progress);
      if (activeRoute.isPresent()) {
         ConvoyRouteDefinition route = activeRoute.get();
         ConvoyRouteDefinition.RouteLeg leg = route.leg(progress.activeRouteLeg());
         diagnostics.add(new EchoDiagnosticBlocker(
            ConvoyTerminalIds.id("diagnostic/active_route"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Convoy route active",
            route.title() + " is on leg " + Math.min(progress.activeRouteLeg() + 1, route.legs().size())
               + "/" + route.legs().size() + ": " + leg.title()
               + ". Marker " + leg.roadsideStructure()
               + ". " + route.threat().label() + " threat profile active.",
            "Drive the paired vehicle to the next roadside signal."
         ));
         if (vehicle == null) {
            diagnostics.add(new EchoDiagnosticBlocker(
               ConvoyTerminalIds.id("diagnostic/paired_vehicle_missing"),
               CHAPTER_ID,
               EchoDiagnosticBlocker.Severity.BLOCKED,
               "Paired convoy vehicle unavailable",
               "The active route expects its paired vehicle, but no owned paired vehicle is available in Core telemetry range.",
               "Move near the paired vehicle or use a Route Beacon on an owned vehicle to repair the link."
            ));
         } else {
            if (vehicle.damage() >= vehicle.maxDamage() * 0.65D) {
               diagnostics.add(new EchoDiagnosticBlocker(
                  ConvoyTerminalIds.id("diagnostic/vehicle_damage"),
                  CHAPTER_ID,
                  EchoDiagnosticBlocker.Severity.WARNING,
                  "Convoy vehicle heavily damaged",
                  vehicle.callsign() + " damage " + vehicle.damage() + "/" + vehicle.maxDamage()
                     + " while assigned to " + route.title() + ".",
                  "Use a Convoy Repair Kit, Vehicle Dock, or Field Repair Station before pushing deeper."
               ));
            }
            int requiredFuel = ConvoyRouteService.requiredFuel(route);
            if (vehicle.fuel() < Math.max(1, requiredFuel / 2)) {
               diagnostics.add(new EchoDiagnosticBlocker(
                  ConvoyTerminalIds.id("diagnostic/vehicle_fuel_low"),
                  CHAPTER_ID,
                  EchoDiagnosticBlocker.Severity.WARNING,
                  "Convoy fuel low",
                  vehicle.callsign() + " fuel " + vehicle.fuel() + "/" + vehicle.maxFuel()
                     + " on " + route.title() + ".",
                  "Refuel at a Vehicle Dock or apply a Fuel Canister before continuing."
               ));
            }
         }
         if (leg.requiresCheckpoint() && checkpointBlocked(player, progress, route)) {
            int reputation = checkpointReputation(player, route);
            diagnostics.add(new EchoDiagnosticBlocker(
               ConvoyTerminalIds.id("diagnostic/checkpoint_blocked"),
               CHAPTER_ID,
               EchoDiagnosticBlocker.Severity.BLOCKED,
               route.checkpoint().label() + " blocked",
               route.checkpoint().blockedMessage(route, reputation),
               "Build faction reputation or choose a route without that checkpoint requirement."
            ));
         }
      }
      if (ConvoyRouteService.claimableRewards(player) > 0) {
         diagnostics.add(new EchoDiagnosticBlocker(
            ConvoyTerminalIds.id("diagnostic/rewards_pending"),
            CHAPTER_ID,
            EchoDiagnosticBlocker.Severity.INFO,
            "Convoy rewards pending",
            "A completed route has unclaimed rewards.",
            "Open FIELD > Convoy Routes and claim the route payout."
         ));
      }
      return List.copyOf(diagnostics);
   }

   private static String routeStatus(Player player, ConvoyProgress progress, ConvoyVehicleEntity vehicle, ConvoyRouteDefinition route) {
      if (progress.claimed(route.id())) {
         return "CLAIMED";
      }
      if (progress.completed(route.id())) {
         return "REWARD READY";
      }
      if (route.id().toString().equals(progress.activeRouteId())) {
         ConvoyRouteDefinition.RouteLeg leg = route.leg(progress.activeRouteLeg());
         if (leg.requiresCheckpoint() && checkpointBlocked(player, progress, route)) {
            return "CHECKPOINT BLOCKED";
         }
         return "ACTIVE LEG " + Math.min(progress.activeRouteLeg() + 1, route.legs().size()) + "/" + route.legs().size();
      }
      ConvoyRouteService.RouteCheck check = ConvoyRouteService.readiness(player, vehicle, route);
      return check.ready() ? "READY" : "BLOCKED: " + check.message();
   }

   private static String routeCategory(ConvoyRouteDefinition route) {
      return "Convoy Route | " + route.requiredVehicle().replace('_', ' ') + " | " + route.threat().label();
   }

   private static String routeDimensionHint(ConvoyProgress progress, ConvoyVehicleEntity vehicle, ConvoyRouteDefinition route) {
      if (!route.id().toString().equals(progress.activeRouteId())) {
         return route.destinationHint();
      }
      ConvoyRouteDefinition.RouteLeg leg = route.leg(progress.activeRouteLeg());
      String remaining = vehicle == null ? "vehicle link missing" : remainingDistance(progress, vehicle.blockPosition(), leg) + "m minimum remaining";
      return route.destinationHint() + " | " + leg.title() + " | " + remaining;
   }

   private static String routeSummary(Player player, ConvoyProgress progress, ConvoyVehicleEntity vehicle, ConvoyRouteDefinition route) {
      StringBuilder summary = new StringBuilder(route.summary());
      summary.append(" Fuel ").append(ConvoyRouteService.requiredFuel(route)).append(". Cargo ").append(cargoSummary(route)).append(".");
      summary.append(" Threat ").append(route.threat().label()).append(".");
      if (route.id().toString().equals(progress.activeRouteId())) {
         ConvoyRouteDefinition.RouteLeg leg = route.leg(progress.activeRouteLeg());
         summary.append(" Next leg: ").append(leg.title()).append(" via ").append(leg.roadsideStructure()).append(".");
         if (leg.requiresCheckpoint()) {
            int reputation = checkpointReputation(player, route);
            summary.append(" Checkpoint ").append(route.checkpoint().label())
               .append(" reputation ").append(reputation).append("/").append(route.minReputation()).append(".");
         }
         if (vehicle != null) {
            summary.append(" Vehicle ").append(vehicle.callsign())
               .append(" fuel ").append(vehicle.fuel()).append("/").append(vehicle.maxFuel())
               .append(", damage ").append(vehicle.damage()).append("/").append(vehicle.maxDamage())
               .append(", cargo ").append(vehicle.filledCargoSlots()).append("/").append(vehicle.cargoSlots()).append(".");
         }
      }
      return summary.toString();
   }

   private static String cargoSummary(ConvoyRouteDefinition route) {
      if (route.requiredCargo().isEmpty()) {
         return "none";
      }
      return route.requiredCargo().stream()
         .map(stack -> stack.itemId().getPath().replace('_', ' ') + " x" + stack.count())
         .reduce((left, right) -> left + ", " + right)
         .orElse("none");
   }

   private static Optional<ConvoyRouteDefinition> activeRoute(ConvoyProgress progress) {
      Identifier routeId = Identifier.tryParse(progress.activeRouteId());
      return routeId == null ? Optional.empty() : ConvoyContent.route(routeId);
   }

   private static ConvoyVehicleEntity relevantVehicle(Player player, ConvoyProgress progress) {
      Optional<UUID> activeVehicle = progress.activeRouteVehicle();
      if (activeVehicle.isPresent() && player.level() instanceof ServerLevel serverLevel) {
         Entity entity = serverLevel.getEntity(activeVehicle.get());
         if (entity instanceof ConvoyVehicleEntity vehicle && vehicle.isOwner(player)) {
            return vehicle;
         }
      }
      return player.level().getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(player.blockPosition()).inflate(16.0D, 6.0D, 16.0D)).stream()
         .filter(vehicle -> vehicle.isOwner(player))
         .min(Comparator.comparingDouble(vehicle -> vehicle.distanceToSqr(player)))
         .orElse(null);
   }

   private static boolean checkpointBlocked(Player player, ConvoyProgress progress, ConvoyRouteDefinition route) {
      if (route.minReputation() <= 0 || progress.checkpointCleared(route.id())) {
         return false;
      }
      return checkpointReputation(player, route) < route.minReputation();
   }

   private static int checkpointReputation(Player player, ConvoyRouteDefinition route) {
      return EchoCoreServices.factionProfile(player, route.checkpointFactionId())
         .map(profile -> profile.reputation())
         .orElse(Integer.MIN_VALUE);
   }

   private static int remainingDistance(ConvoyProgress progress, BlockPos current, ConvoyRouteDefinition.RouteLeg leg) {
      int required = leg.minDistanceFromStart();
      if (required <= 0) {
         return 0;
      }
      Optional<BlockPos> start = progress.activeRouteStart();
      if (start.isEmpty()) {
         return required;
      }
      long dx = (long)start.get().getX() - current.getX();
      long dz = (long)start.get().getZ() - current.getZ();
      int traveled = (int)Math.floor(Math.sqrt(dx * dx + dz * dz));
      return Math.max(0, required - traveled);
   }
}
