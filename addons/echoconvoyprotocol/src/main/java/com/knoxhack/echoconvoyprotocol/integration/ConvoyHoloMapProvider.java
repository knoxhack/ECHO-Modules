package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.Config;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.world.ConvoyRouteMarkerIndex;
import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echomultiblockcore.runtime.MultiblockRuntimeManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public enum ConvoyHoloMapProvider implements IMapDataProvider {
   INSTANCE;

   public static final Identifier FACILITIES = id("convoy_facilities");
   public static final Identifier ROUTES = id("convoy_routes");
   public static final Identifier RECOVERY = id("recovery_signals");

   @Override
   public Identifier providerId() {
      return id("convoy_holomap");
   }

   @Override
   public List<IMapLayer> layers(Player player) {
      return List.of(
         new EchoMapLayer(FACILITIES, "Convoy Facilities", 95, 0xFF6F8F5F, true),
         new EchoMapLayer(ROUTES, "Convoy Routes", 96, 0xFF00D8FF, true),
         new EchoMapLayer(RECOVERY, "Recovery Signals", 97, 0xFFFF8A2A, true)
      );
   }

   @Override
   public List<IMapMarker> markers(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return List.of();
      }
      if (!(serverPlayer.level() instanceof ServerLevel level)) {
         return List.of();
      }
      List<IMapMarker> markers = new ArrayList<>();
      facilityMarkers(level, markers);
      routeMarkers(serverPlayer, level, markers);
      return markers;
   }

   @Override
   public boolean refresh(ServerPlayer player, String reason) {
      return player != null;
   }

   private void facilityMarkers(ServerLevel level, List<IMapMarker> markers) {
      for (var entry : MultiblockRuntimeManager.formed(level)) {
         if (entry.definitionId() == null || !EchoConvoyProtocol.MODID.equals(entry.definitionId().getNamespace())) {
            continue;
         }
         boolean recovery = entry.definitionId().getPath().contains("recovery");
         Identifier layer = recovery ? RECOVERY : FACILITIES;
         IMapMarker.MarkerState state = "OFFLINE".equals(entry.state())
            ? IMapMarker.MarkerState.HIDDEN
            : ("UNBUILT".equals(entry.state()) || "INCOMPLETE".equals(entry.state())
               ? IMapMarker.MarkerState.LOCKED
               : IMapMarker.MarkerState.DISCOVERED);
         BlockPos pos = entry.controllerPos();
         String title = title(entry.definitionId().getPath());
         markers.add(new EchoMapMarker(
            id("facility/" + entry.definitionId().getPath() + "/" + pos.asLong()),
            layer,
            providerId(),
            recovery ? IMapMarker.MarkerKind.MISSION : IMapMarker.MarkerKind.BASE_OUTPOST,
            state,
            title,
            "Integrity " + Math.round(entry.integrity()) + "% | State " + entry.state(),
            level.dimension(),
            pos.getX() + 0.5D,
            pos.getY(),
            pos.getZ() + 0.5D,
            28.0F,
            id("icon/" + entry.definitionId().getPath()),
            null,
            -1,
            true
         ));
         if (level.getBlockEntity(pos) instanceof ConvoyMultiblockControllerBlockEntity controller
            && controller.convoyState().damagedConvoy()) {
            markers.add(recoveryMarker(level, pos, controller.convoyState().activeRouteId()));
         }
         if (level.getBlockEntity(pos) instanceof ConvoyMultiblockControllerBlockEntity controller) {
            operationMarkers(level, pos, controller, markers);
         }
      }
   }

   private void operationMarkers(ServerLevel level, BlockPos depotPos, ConvoyMultiblockControllerBlockEntity controller, List<IMapMarker> markers) {
      var operation = controller.fieldOperation();
      Identifier routeId = operation.routeIdentifier();
      if (routeId == null || operation.routeId().isBlank()) {
         return;
      }
      ConvoyRouteDefinition route = ConvoyContent.route(routeId).orElse(null);
      if (route == null) {
         return;
      }
      int order = Math.max(0, operation.currentStage());
      BlockPos pos = fallbackPos(depotPos, route, route.leg(Math.min(order, route.legs().size() - 1)), order);
      IMapMarker.MarkerState state = switch (operation.phase()) {
         case COMPLETE, RECOVERED -> IMapMarker.MarkerState.CHECKED;
         case FAILED, RECOVERY_PENDING, INCIDENT_BLOCKED -> IMapMarker.MarkerState.DISCOVERED;
         case STAGED, EN_ROUTE, AWAITING_SIGNAL, RETURNING -> IMapMarker.MarkerState.DISCOVERED;
         default -> IMapMarker.MarkerState.HIDDEN;
      };
      markers.add(new EchoMapMarker(
         id("operation/" + operation.operationId() + "/" + order),
         route.holomapLayerId(),
         providerId(),
         operation.phase().name().contains("INCIDENT") ? IMapMarker.MarkerKind.HAZARD : IMapMarker.MarkerKind.ROUTE,
         state,
         "Convoy Operation: " + route.title(),
         operation.summaryLine(),
         level.dimension(),
         pos.getX() + 0.5D,
         pos.getY(),
         pos.getZ() + 0.5D,
         operation.phase().name().contains("INCIDENT") ? 64.0F : 32.0F,
         icon(operation.incidentId().isBlank() ? route.holomapIcon() : "field_incident"),
         route.id(),
         order,
         false
      ));
      if (!operation.incidentId().isBlank()) {
         markers.add(new EchoMapMarker(
            id("operation_incident/" + operation.operationId()),
            RECOVERY,
            providerId(),
            IMapMarker.MarkerKind.HAZARD,
            IMapMarker.MarkerState.DISCOVERED,
            "Field Incident",
            operation.failureReason(),
            level.dimension(),
            pos.getX() + 0.5D,
            pos.getY(),
            pos.getZ() + 0.5D,
            80.0F,
            id("icon/field_incident"),
            route.id(),
            order,
            false
         ));
      }
      if (operation.recoveryMarker()) {
         markers.add(recoveryMarker(level, pos, route.id().toString()));
      }
   }

   private void routeMarkers(ServerPlayer player, ServerLevel level, List<IMapMarker> markers) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      Identifier activeRoute = Identifier.tryParse(progress.activeRouteId());
      BlockPos base = progress.activeRouteStart().orElse(player.blockPosition());
      ConvoyRouteMarkerIndex index = ConvoyRouteMarkerIndex.get(level);
      for (ConvoyRouteDefinition route : ConvoyContent.routes()) {
         IMapMarker.MarkerState state = routeState(progress, route, activeRoute);
         List<ConvoyRouteMarkerIndex.Entry> known = index.routeEntries(route.id());
         if (!known.isEmpty()) {
            for (ConvoyRouteMarkerIndex.Entry entry : known) {
               markers.add(routeMarker(level, route, entry.pos(), entry.order(), state, Config.preciseHoloMapMarkers(), 24.0F, entry.legId()));
            }
            continue;
         }
         if (route.id().equals(activeRoute)) {
            for (int i = 0; i < route.legs().size(); i++) {
               ConvoyRouteDefinition.RouteLeg leg = route.leg(i);
               markers.add(routeMarker(level, route, fallbackPos(base, route, leg, i), i, state, false, 96.0F, leg.id()));
            }
         } else {
            markers.add(routeMarker(level, route, fallbackPos(player.blockPosition(), route, route.leg(0), 0), 0, state, false, 128.0F, "search_zone"));
         }
         if (route.threatLevel() >= 4) {
            markers.add(highRiskMarker(level, player.blockPosition(), route));
         }
      }
   }

   private IMapMarker routeMarker(ServerLevel level, ConvoyRouteDefinition route, BlockPos pos, int order, IMapMarker.MarkerState state, boolean precise, float radius, String legId) {
      return new EchoMapMarker(
         id("route/" + route.id().getPath() + "/" + legId + "/" + order),
         route.holomapLayerId(),
         providerId(),
         IMapMarker.MarkerKind.ROUTE,
         state,
         route.title(),
         route.missionType() + " | threat " + route.threatLevel() + " | logistics " + route.logisticsNetworkId(),
         level.dimension(),
         pos.getX() + 0.5D,
         pos.getY(),
         pos.getZ() + 0.5D,
         radius,
         icon(route.holomapIcon()),
         route.id(),
         order,
         precise
      );
   }

   private IMapMarker recoveryMarker(ServerLevel level, BlockPos pos, String routeId) {
      return new EchoMapMarker(
         id("recovery/failed_convoy/" + pos.asLong()),
         RECOVERY,
         providerId(),
         IMapMarker.MarkerKind.MISSION,
         IMapMarker.MarkerState.DISCOVERED,
         "Failed Convoy Signal",
         "Recovery task required" + (routeId == null || routeId.isBlank() ? "." : " for " + routeId + "."),
         level.dimension(),
         pos.getX() + 0.5D,
         pos.getY(),
         pos.getZ() + 0.5D,
         48.0F,
         id("icon/recovery_signal"),
         Identifier.tryParse(routeId == null ? "" : routeId),
         999,
         true
      );
   }

   private IMapMarker highRiskMarker(ServerLevel level, BlockPos origin, ConvoyRouteDefinition route) {
      BlockPos pos = fallbackPos(origin, route, route.leg(0), 0).offset(0, 0, 32);
      return new EchoMapMarker(
         id("risk/" + route.id().getPath()),
         ROUTES,
         providerId(),
         IMapMarker.MarkerKind.HAZARD,
         IMapMarker.MarkerState.DISCOVERED,
         "High-Risk Delivery Zone",
         route.title() + " threat level " + route.threatLevel(),
         level.dimension(),
         pos.getX() + 0.5D,
         pos.getY(),
         pos.getZ() + 0.5D,
         160.0F,
         id("icon/high_risk_delivery"),
         route.id(),
         1000,
         false
      );
   }

   private static IMapMarker.MarkerState routeState(ConvoyProgress progress, ConvoyRouteDefinition route, Identifier activeRoute) {
      if (progress.claimed(route.id()) || progress.completed(route.id())) {
         return IMapMarker.MarkerState.CHECKED;
      }
      if (route.id().equals(activeRoute) || route.unlockRequirement().isBlank()) {
         return IMapMarker.MarkerState.DISCOVERED;
      }
      return IMapMarker.MarkerState.LOCKED;
   }

   private static BlockPos fallbackPos(BlockPos origin, ConvoyRouteDefinition route, ConvoyRouteDefinition.RouteLeg leg, int order) {
      int distance = Math.max(48, leg.minDistanceFromStart());
      int lateral = ((route.order() + order * 17) % 7 - 3) * 24;
      return origin.offset(distance, 0, lateral);
   }

   private static Identifier icon(String raw) {
      if (raw != null && raw.contains(":")) {
         Identifier parsed = Identifier.tryParse(raw);
         if (parsed != null) {
            return parsed;
         }
      }
      return id("icon/" + (raw == null || raw.isBlank() ? "convoy_route" : raw));
   }

   private static String title(String path) {
      String[] parts = path.split("_");
      StringBuilder title = new StringBuilder();
      for (String part : parts) {
         if (title.length() > 0) {
            title.append(' ');
         }
         title.append(part.isBlank() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1));
      }
      return title.toString();
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, path);
   }
}
