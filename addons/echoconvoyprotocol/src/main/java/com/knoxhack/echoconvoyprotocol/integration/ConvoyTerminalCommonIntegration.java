package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalStatePacket;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalSync;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class ConvoyTerminalCommonIntegration {
   private ConvoyTerminalCommonIntegration() {
   }

   public static void register() {
      TerminalActionRegistry.register(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.SCAN_ACTION, (player, payload) -> {
         if (player != null) {
            long claimable = ConvoyRouteService.claimableRewards(player);
            player.sendSystemMessage(Component.literal("ECHO CONVOY // Routes " + ConvoyContent.routes().size() + ", rewards " + claimable + "."));
            if (player instanceof ServerPlayer serverPlayer) {
               EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
               ConvoyTerminalStatePacket snapshot = ConvoyTerminalStatePacket.from(serverPlayer);
               String guidance = snapshot.assistantLines().isEmpty() ? snapshot.assistantStatus() : snapshot.assistantLines().getFirst();
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // Field Assistant: " + guidance));
               ConvoyTerminalSync.send(serverPlayer);
            }
         }
      });
      TerminalActionRegistry.register(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.START_ACTION, (player, payload) -> {
         if (player instanceof ServerPlayer serverPlayer) {
            Identifier routeId = routePayload(payload);
            if (routeId == null) {
               routeId = firstStartableRoute(serverPlayer).map(ConvoyRouteDefinition::id).orElse(null);
            }
            if (routeId == null) {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // No startable convoy route selected. " + routeBlockerHint(serverPlayer)));
            } else if (!ConvoyProgress.get(serverPlayer).activeRouteId().isBlank()) {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // Finish the active convoy route before starting another. Use SIGNAL at the next matching Roadside Signal Marker."));
            } else if (ConvoyContent.route(routeId).isEmpty()) {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // Selected convoy route is not loaded: " + routeId + "."));
            } else if (ConvoyProgress.get(serverPlayer).completed(routeId)) {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // Selected convoy route is already complete."));
            } else {
               ConvoyRouteService.activateRoute(serverPlayer, nearestVehicle(serverPlayer), routeId);
            }
            ConvoyTerminalSync.send(serverPlayer);
         }
      });
      TerminalActionRegistry.register(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.COMPLETE_ACTION, (player, payload) -> {
         if (player instanceof ServerPlayer serverPlayer) {
            if (ConvoyProgress.get(serverPlayer).activeRouteId().isBlank()) {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // No active route to advance."));
               return;
            }
            SignalMarker marker = nearestSignalMarker(serverPlayer);
            if (marker == null) {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // No Roadside Signal Marker in terminal range. Drive to the route marker, satisfy the distance requirement, then scan again."));
               return;
            }
            ConvoyRouteService.advanceRouteAtSignal(serverPlayer, nearestVehicle(serverPlayer), marker.pos(), marker.station());
            ConvoyTerminalSync.send(serverPlayer);
         }
      });
      TerminalActionRegistry.register(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.CLAIM_ACTION, (player, payload) -> {
         if (player instanceof ServerPlayer serverPlayer) {
            Identifier routeId = routePayload(payload);
            if (routeId == null) {
               routeId = ConvoyContent.routes().stream()
                  .filter(route -> ConvoyProgress.get(serverPlayer).completed(route.id()) && !ConvoyProgress.get(serverPlayer).claimed(route.id()))
                  .map(route -> route.id())
                  .findFirst()
                  .orElse(null);
            }
            if (routeId != null) {
               ConvoyRouteService.claimRouteRewards(serverPlayer, routeId);
            } else {
               serverPlayer.sendSystemMessage(Component.literal("ECHO CONVOY // No completed convoy route has unclaimed rewards."));
            }
            ConvoyTerminalSync.send(serverPlayer);
         }
      });
      boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
      if (missionCoreLoaded) {
         ConvoyMissionCoreIntegration.register();
      } else {
         TerminalMissionRegistry.register(ConvoyMissionProvider.INSTANCE);
      }
      TerminalMissionActions.registerForTab(ConvoyTerminalIds.CONVOY_TAB);
      registerArchive();
      EchoConvoyProtocol.LOGGER.info("ECHO Convoy Protocol terminal actions registered.");
   }

   private static ConvoyVehicleEntity nearestVehicle(ServerPlayer player) {
      return player.level().getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(player.blockPosition()).inflate(6.0D)).stream()
         .filter(vehicle -> vehicle.isOwner(player))
         .min((left, right) -> Double.compare(left.distanceToSqr(player), right.distanceToSqr(player)))
         .orElse(null);
   }

   @Nullable
   private static Identifier routePayload(String payload) {
      return payload == null || payload.isBlank() ? null : Identifier.tryParse(payload);
   }

   private static Optional<ConvoyRouteDefinition> firstStartableRoute(ServerPlayer player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      if (!progress.activeRouteId().isBlank()) {
         return Optional.empty();
      }
      ConvoyVehicleEntity vehicle = nearestVehicle(player);
      return ConvoyContent.routes().stream()
         .filter(route -> !progress.completed(route.id()))
         .filter(route -> ConvoyRouteService.readiness(player, vehicle, route).ready())
         .findFirst();
   }

   private static String routeBlockerHint(ServerPlayer player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      ConvoyVehicleEntity vehicle = nearestVehicle(player);
      return ConvoyContent.routes().stream()
         .filter(route -> !progress.completed(route.id()) && !progress.claimed(route.id()))
         .findFirst()
         .map(route -> ConvoyRouteService.readinessHint(player, vehicle, route))
         .orElse("No unclaimed route definitions are loaded.");
   }

   @Nullable
   private static SignalMarker nearestSignalMarker(ServerPlayer player) {
      Level level = player.level();
      BlockPos center = player.blockPosition();
      SignalMarker best = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -3, -6), center.offset(6, 3, 6))) {
         BlockPos candidate = pos.immutable();
         if (!level.getBlockState(candidate).is(ModBlocks.ROADSIDE_SIGNAL_MARKER.get())) {
            continue;
         }
         double distance = distanceToCenterSqr(candidate, player);
         if (distance < bestDistance) {
            bestDistance = distance;
            ConvoyStationBlockEntity station = level.getBlockEntity(candidate) instanceof ConvoyStationBlockEntity value ? value : null;
            best = new SignalMarker(candidate, station);
         }
      }
      return best;
   }

   private static double distanceToCenterSqr(BlockPos pos, ServerPlayer player) {
      double dx = pos.getX() + 0.5D - player.getX();
      double dy = pos.getY() + 0.5D - player.getY();
      double dz = pos.getZ() + 0.5D - player.getZ();
      return dx * dx + dy * dy + dz * dz;
   }

   private static void registerArchive() {
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(ConvoyTerminalIds.id("archive/field_convoys"),
         "Convoy Protocol", "Field Convoys", "ROUTE READY",
         List.of("Convoy vehicles persist fuel, damage, ownership, and cargo between field runs.",
            "Roadside Signal Markers close active routes and keep rewards idempotent."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(ConvoyTerminalIds.id("archive/checkpoints"),
         "Convoy Protocol", "Faction Checkpoints", "GUARDED",
         List.of("Checkpoint routes can require ECHO Core faction reputation.",
            "Operators should prep fuel, cargo, water, repair kits, and route beacons before departure."), false));
   }

   private record SignalMarker(BlockPos pos, @Nullable ConvoyStationBlockEntity station) {
   }
}
