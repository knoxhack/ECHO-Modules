package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalStatePacket;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalSync;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionVisuals;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class ConvoyMissionProvider implements TerminalMissionProvider {
   public static final ConvoyMissionProvider INSTANCE = new ConvoyMissionProvider();
   public static final Identifier PREP_VEHICLE = ConvoyTerminalIds.id("prep_vehicle");
   public static final Identifier START_ROUTE = ConvoyTerminalIds.id("start_route_mission");
   public static final Identifier CLOSE_ROUTE = ConvoyTerminalIds.id("close_route");
   public static final Identifier DEPOT_FORMATION = ConvoyTerminalIds.id("depot_formation");
   public static final Identifier REFUEL_REPAIR = ConvoyTerminalIds.id("refuel_repair");
   public static final Identifier FIELD_OPERATION_STAGING = ConvoyTerminalIds.id("field_operation_staging");
   public static final Identifier INCIDENT_RESOLUTION = ConvoyTerminalIds.id("incident_resolution");
   public static final Identifier CONVOY_RECOVERY = ConvoyTerminalIds.id("convoy_recovery");
   public static final Identifier SALVAGE_EXPORT = ConvoyTerminalIds.id("salvage_export");
   private static final Identifier ASHFALL_EXPEDITION_READINESS =
      Identifier.fromNamespaceAndPath("echoashfallprotocol", "expedition_readiness");

   private static final String ACTION_SCAN = "scan_routes";
   private static final String ACTION_START = "start_ready_route";
   private static final String ACTION_SIGNAL = "log_signal_marker";
   private static final String ACTION_CLAIM = "claim_route_reward";
   private static final int ACCENT = 0x92D66B;
   private static final String COPY = "terminal.echoconvoyprotocol.mission.";
   private static final List<SideMission> SIDE_MISSIONS = List.of(
      new SideMission(DEPOT_FORMATION, "depot_formation", "Depot Formation", "Validate a Convoy Depot controller and bring the depot network online.", "Build or revalidate a Convoy Depot multiblock near your route crew.", "Depot Ops", "Depot", 40, "Convoy depot validated"),
      new SideMission(REFUEL_REPAIR, "refuel_repair", "Refuel And Repair", "Use a dock, field station, or maintenance kit to restore a convoy vehicle.", "Top up fuel or repair damage before staging a longer field operation.", "Vehicle Support", "Field", 50, "Vehicle support completed"),
      new SideMission(FIELD_OPERATION_STAGING, "field_operation_staging", "Stage Field Operation", "Stage or launch a depot-backed field operation for a Convoy route.", "Use the depot operation tasks to prepare the route, cargo, and dispatch plan.", "Field Ops", "Field", 60, "Field operation staged"),
      new SideMission(INCIDENT_RESOLUTION, "incident_resolution", "Resolve Road Incident", "Clear a field incident so the convoy can continue its operation.", "Run the incident response task after an operation reports a blocker.", "Field Ops", "Hazard", 70, "Incident resolved"),
      new SideMission(CONVOY_RECOVERY, "convoy_recovery", "Recovery Beacon", "Recover a recalled or failed convoy operation back to depot state.", "Use depot recovery when damage or recall strands a field operation.", "Recovery", "Hazard", 80, "Convoy recovered"),
      new SideMission(SALVAGE_EXPORT, "salvage_export", "Salvage Export", "Export a completed route salvage manifest into Logistics or a depot output crate.", "Close the route, unload salvage, and export the return manifest once.", "Closeout", "Field", 90, "Salvage exported")
   );

   private ConvoyMissionProvider() {
   }

   @Override
   public TerminalMissionChapter chapter() {
      return new TerminalMissionChapter(
         ConvoyTerminalIds.CONVOY_TAB,
         tr("chapter.title"),
         tr("chapter.summary"),
         260,
         ACCENT,
         true
      );
   }

   @Override
   public List<TerminalMissionDefinition> missions(Player player) {
      List<TerminalMissionDefinition> definitions = new ArrayList<>();
      definitions.add(prepDefinition(player));
      definitions.add(routeDefinition(player));
      definitions.add(closeDefinition(player));
      SIDE_MISSIONS.stream().map(side -> sideDefinition(player, side)).forEach(definitions::add);
      return List.copyOf(definitions);
   }

   @Override
   public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
      if (PREP_VEHICLE.equals(missionId)) {
         return prepSnapshot(player);
      }
      if (START_ROUTE.equals(missionId)) {
         return routeSnapshot(player);
      }
      if (CLOSE_ROUTE.equals(missionId)) {
         return closeSnapshot(player);
      }
      SideMission side = sideMission(missionId);
      if (side != null) {
         return sideSnapshot(player, side);
      }
      return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F, tr("status.unknown"), "", "", List.of());
   }

   @Override
   public TerminalMissionPresentation presentation(
      Player player,
      TerminalMissionDefinition definition,
      TerminalMissionSnapshot snapshot
   ) {
      String next = snapshot == null ? "" : snapshot.actionHint();
      RouteGuidance guidance = routeGuidance(player);
      List<String> tags = new ArrayList<>();
      tags.add(tr("tag.field_assistant"));
      tags.add(definition.category());
      tags.add(definition.difficulty());
      if (START_ROUTE.equals(definition.id())) {
         tags.add(guidance.blocker().label());
         guidance.route().ifPresent(route -> tags.add(route.requiredVehicle().replace('_', ' ')));
      } else if (CLOSE_ROUTE.equals(definition.id())) {
         tags.add(closeTone(player).label());
      }
      return new TerminalMissionPresentation(
         definition.title(),
         definition.briefing(),
         next,
         START_ROUTE.equals(definition.id()) ? guidance.blocker().routeHint() : tr("route_hint.convoy_routes"),
         missionTone(player, definition.id(), snapshot),
         tags,
         ""
      );
   }

   @Override
   public TerminalMissionVisuals visuals(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      if (PREP_VEHICLE.equals(definition.id())) {
         return new TerminalMissionVisuals(TerminalVisualAssets.MISSION_CRAFTING, "crafting", "vehicle_prep", missionTone(player, definition.id(), snapshot));
      }
      if (START_ROUTE.equals(definition.id())) {
         BlockerKind blocker = routeGuidance(player).blocker();
         return new TerminalMissionVisuals(blocker.categoryArt(), blocker.trackType(), blocker.heroVariant(), blocker.visualTone());
      }
      if (CLOSE_ROUTE.equals(definition.id())) {
         BlockerKind closeTone = closeTone(player);
         return new TerminalMissionVisuals(closeTone.categoryArt(), closeTone.trackType(), closeTone.heroVariant(), closeTone.visualTone());
      }
      return TerminalMissionVisuals.fallback(definition, snapshot);
   }

   @Override
   public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      return TerminalMissionRole.OPTIONAL;
   }

   @Override
   public Optional<TerminalMissionRoutePlacement> routePlacement(
      Player player,
      TerminalMissionDefinition definition,
      TerminalMissionSnapshot snapshot,
      TerminalMissionRole role
   ) {
      int order = definition == null ? 0 : definition.missionOrder();
      return Optional.of(TerminalMissionRoutePlacement.optional(
         routePhase(definition == null ? null : definition.id()), order));
   }

   @Override
   public List<Identifier> routePrerequisites(
      Player player,
      TerminalMissionDefinition definition,
      TerminalMissionSnapshot snapshot,
      TerminalMissionRole role
   ) {
      if (definition == null) {
         return List.of();
      }
      return List.of(ASHFALL_EXPEDITION_READINESS);
   }

   @Override
   public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
      if (player == null) {
         return true;
      }
      if (ACTION_SCAN.equals(actionId)) {
         EchoCoreServices.discoverVisibleRouteRecords(player);
         ConvoyTerminalStatePacket snapshot = ConvoyTerminalStatePacket.from(player);
         String detail = snapshot.assistantLines().isEmpty() ? snapshot.assistantStatus() : snapshot.assistantLines().getFirst();
         player.sendSystemMessage(Component.literal(tr("message.assistant", snapshot.assistantStatus(), detail)));
         ConvoyTerminalSync.send(player);
         return true;
      }
      if (ACTION_START.equals(actionId)) {
         Optional<ConvoyRouteDefinition> ready = firstStartableRoute(player);
         if (ready.isEmpty()) {
            player.sendSystemMessage(Component.literal(tr("message.no_ready_route", routeBlockerHint(player))));
            ConvoyTerminalSync.send(player);
            return true;
         }
         ConvoyRouteService.activateRoute(player, nearestVehicle(player, 8.0D), ready.get().id());
         ConvoyTerminalSync.send(player);
         return true;
      }
      if (ACTION_SIGNAL.equals(actionId)) {
         SignalMarker marker = nearestSignalMarker(player);
         if (marker == null) {
            player.sendSystemMessage(Component.literal(tr("message.no_signal_marker")));
            ConvoyTerminalSync.send(player);
            return true;
         }
         ConvoyRouteService.advanceRouteAtSignal(player, nearestVehicle(player, 8.0D), marker.pos(), marker.station());
         ConvoyTerminalSync.send(player);
         return true;
      }
      if (ACTION_CLAIM.equals(actionId)) {
         Optional<ConvoyRouteDefinition> reward = firstClaimableRoute(player);
         if (reward.isEmpty()) {
            player.sendSystemMessage(Component.literal(tr("message.no_reward")));
            ConvoyTerminalSync.send(player);
            return true;
         }
         ConvoyRouteService.claimRouteRewards(player, reward.get().id());
         ConvoyTerminalSync.send(player);
         return true;
      }
      return false;
   }

   private static TerminalMissionDefinition prepDefinition(Player player) {
      return new TerminalMissionDefinition(
         PREP_VEHICLE,
         ConvoyTerminalIds.CONVOY_TAB,
         "field_assistant",
         tr("phase.field_assistant"),
         10,
         10,
         tr("prep.title"),
         tr("prep.briefing"),
         tr("prep.field_guide"),
         tr("category.acquisition"),
         tr("difficulty.starter"),
         new ItemStack(ModBlocks.VEHICLE_WORKBENCH.get().asItem()),
         List.of(),
         prepRequirements(player),
         List.of(TerminalMissionReward.text(tr("reward.route_board.label"), tr("reward.route_board.detail")))
      );
   }

   private static int routePhase(Identifier missionId) {
      if (PREP_VEHICLE.equals(missionId) || START_ROUTE.equals(missionId) || DEPOT_FORMATION.equals(missionId)) {
         return 8;
      }
      if (CLOSE_ROUTE.equals(missionId) || REFUEL_REPAIR.equals(missionId)
         || FIELD_OPERATION_STAGING.equals(missionId)) {
         return 10;
      }
      if (INCIDENT_RESOLUTION.equals(missionId) || CONVOY_RECOVERY.equals(missionId)) {
         return 12;
      }
      if (SALVAGE_EXPORT.equals(missionId)) {
         return 15;
      }
      return 10;
   }

   private static TerminalMissionDefinition routeDefinition(Player player) {
      Optional<ConvoyRouteDefinition> route = firstIncompleteRoute(player);
      return new TerminalMissionDefinition(
         START_ROUTE,
         ConvoyTerminalIds.CONVOY_TAB,
         "field_assistant",
         tr("phase.field_assistant"),
         10,
         20,
         tr("route.title"),
         tr("route.briefing"),
         tr("route.field_guide"),
         tr("category.route_prep"),
         route.map(ConvoyRouteDefinition::requiredVehicle).orElse(tr("difficulty.any_vehicle")),
         routeIcon(player, route.orElse(null)),
         List.of(PREP_VEHICLE.toString()),
         routeRequirements(player, route.orElse(null)),
         route.map(definition -> List.of(TerminalMissionReward.text(definition.title(), definition.summary()))).orElse(List.of())
      );
   }

   private static TerminalMissionDefinition closeDefinition(Player player) {
      Optional<ConvoyRouteDefinition> active = activeRoute(player);
      return new TerminalMissionDefinition(
         CLOSE_ROUTE,
         ConvoyTerminalIds.CONVOY_TAB,
         "field_assistant",
         tr("phase.field_assistant"),
         10,
         30,
         tr("close.title"),
         tr("close.briefing"),
         tr("close.field_guide"),
         tr("category.closeout"),
         active.map(route -> route.threat().label()).orElse(tr("difficulty.field")),
         closeIcon(player, active.orElse(null)),
         List.of(START_ROUTE.toString()),
         closeRequirements(player, active.orElse(null)),
         active.map(route -> route.rewards().stream().map(spec -> TerminalMissionReward.of(spec.stack())).toList()).orElse(List.of())
      );
   }

   private static TerminalMissionDefinition sideDefinition(Player player, SideMission side) {
      boolean complete = sideComplete(player, side);
      ItemStack icon = sideIcon(side.key());
      return new TerminalMissionDefinition(
         side.id(),
         ConvoyTerminalIds.CONVOY_TAB,
         "side_ops",
         side.category(),
         20,
         side.order(),
         side.title(),
         side.briefing(),
         side.guide(),
         side.category(),
         side.difficulty(),
         icon,
         List.of(PREP_VEHICLE.toString()),
         List.of(TerminalMissionRequirement.custom(side.requirement(), side.guide(), icon.copy(), complete ? 1 : 0, 1, complete)),
         List.of(TerminalMissionReward.text(side.title(), side.requirement()))
      );
   }

   private static TerminalMissionSnapshot prepSnapshot(Player player) {
      ConvoyVehicleEntity vehicle = nearestVehicle(player, 12.0D);
      boolean progressed = hasAnyRouteProgress(player);
      boolean complete = vehicle != null || progressed;
      boolean hasWorkbench = hasWorkbench(player);
      boolean hasKit = inventoryCount(player, ModItems.SCRAP_BIKE_KIT.get()) > 0;
      float progress = complete ? 1.0F : hasKit ? 0.75F : hasWorkbench ? 0.35F : 0.0F;
      String hint = complete
         ? tr("prep.hint.complete")
         : hasKit
            ? tr("prep.hint.deploy_kit")
            : hasWorkbench
               ? tr("prep.hint.process_kit")
               : tr("prep.hint.craft_workbench");
      return new TerminalMissionSnapshot(
         PREP_VEHICLE,
         complete ? TerminalMissionStatus.COMPLETED : TerminalMissionStatus.UNLOCKED,
         progress,
         complete ? tr("prep.status.complete") : tr("prep.status.needed"),
         "",
         hint,
         List.of(TerminalMissionAction.enabled(ACTION_SCAN, tr("action.scan_convoy")))
      );
   }

   private static TerminalMissionSnapshot routeSnapshot(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      ConvoyVehicleEntity vehicle = nearestVehicle(player, 12.0D);
      Optional<ConvoyRouteDefinition> ready = firstStartableRoute(player);
      boolean active = !progress.activeRouteId().isBlank();
      boolean complete = hasAnyCompletedRoute(progress);
      String hint = active
         ? tr("route.hint.active")
         : ready.map(route -> tr("route.hint.ready", route.title()))
            .orElse(routeBlockerHint(player));
      List<TerminalMissionAction> actions = new ArrayList<>();
      actions.add(TerminalMissionAction.enabled(ACTION_SCAN, tr("action.scan_routes")));
      actions.add(ready.isPresent() && !active
         ? TerminalMissionAction.enabled(ACTION_START, tr("action.start_ready_route"))
         : TerminalMissionAction.disabled(ACTION_START, tr("action.start_ready_route"), active ? tr("disabled.active_route") : routeBlockerHint(player)));
      return new TerminalMissionSnapshot(
         START_ROUTE,
         active || complete ? TerminalMissionStatus.COMPLETED : TerminalMissionStatus.UNLOCKED,
         active || complete ? 1.0F : ready.isPresent() ? 0.85F : vehicle != null ? 0.35F : 0.0F,
         active ? tr("route.status.active") : ready.isPresent() ? tr("route.status.ready") : tr("route.status.blocked"),
         "",
         hint,
         actions
      );
   }

   private static TerminalMissionSnapshot closeSnapshot(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      Optional<ConvoyRouteDefinition> active = activeRoute(player);
      Optional<ConvoyRouteDefinition> claimable = firstClaimableRoute(player);
      boolean allClaimed = allRoutesClaimed(progress);
      List<TerminalMissionAction> actions = new ArrayList<>();
      actions.add(active.isPresent()
         ? TerminalMissionAction.enabled(ACTION_SIGNAL, tr("action.log_signal"))
         : TerminalMissionAction.disabled(ACTION_SIGNAL, tr("action.log_signal"), tr("disabled.start_route_first")));
      actions.add(claimable.isPresent()
         ? TerminalMissionAction.enabled(ACTION_CLAIM, tr("action.claim_reward"))
         : TerminalMissionAction.disabled(ACTION_CLAIM, tr("action.claim_reward"), tr("disabled.no_reward")));
      TerminalMissionStatus status = claimable.isPresent()
         ? TerminalMissionStatus.CLAIMABLE
         : allClaimed ? TerminalMissionStatus.CLAIMED : active.isPresent() ? TerminalMissionStatus.UNLOCKED : TerminalMissionStatus.LOCKED;
      String hint = claimable.map(route -> tr("close.hint.claim", route.title()))
         .orElse(active.map(ConvoyMissionProvider::activeRouteHint).orElse(tr("close.hint.locked")));
      return new TerminalMissionSnapshot(
         CLOSE_ROUTE,
         status,
         claimable.isPresent() || allClaimed ? 1.0F : active.isPresent() ? 0.55F : 0.0F,
         claimable.isPresent() ? tr("close.status.reward") : allClaimed ? tr("close.status.claimed") : active.isPresent() ? tr("close.status.signal") : tr("status.locked"),
         active.isEmpty() && claimable.isEmpty() && !allClaimed ? tr("disabled.start_route_first") : "",
         hint,
         actions
      );
   }

   private static TerminalMissionSnapshot sideSnapshot(Player player, SideMission side) {
      boolean complete = sideComplete(player, side);
      return new TerminalMissionSnapshot(
         side.id(),
         complete ? TerminalMissionStatus.COMPLETED : TerminalMissionStatus.UNLOCKED,
         complete ? 1.0F : 0.0F,
         complete ? "Complete" : "Active",
         "",
         complete ? side.requirement() : side.guide(),
         List.of(TerminalMissionAction.enabled(ACTION_SCAN, tr("action.scan_routes")))
      );
   }

   private static List<TerminalMissionRequirement> prepRequirements(Player player) {
      boolean workbench = hasWorkbench(player);
      int kitCount = inventoryCount(player, ModItems.SCRAP_BIKE_KIT.get());
      boolean vehicle = nearestVehicle(player, 12.0D) != null || hasAnyRouteProgress(player);
      return List.of(
         TerminalMissionRequirement.block(tr("requirement.workbench"), tr("requirement.workbench.detail"), new ItemStack(ModBlocks.VEHICLE_WORKBENCH.get().asItem()), workbench ? 1 : 0, 1, workbench),
         TerminalMissionRequirement.item(new ItemStack(ModItems.SCRAP_BIKE_KIT.get()), kitCount, 1, kitCount > 0 || vehicle),
         TerminalMissionRequirement.custom(tr("requirement.owned_vehicle"), tr("requirement.owned_vehicle.detail"), new ItemStack(ModItems.SCRAP_BIKE_KIT.get()), vehicle ? 1 : 0, 1, vehicle)
      );
   }

   private static List<TerminalMissionRequirement> routeRequirements(Player player, @Nullable ConvoyRouteDefinition route) {
      ConvoyVehicleEntity vehicle = nearestVehicle(player, 12.0D);
      if (route == null) {
         return List.of(TerminalMissionRequirement.custom(tr("requirement.route_data"), tr("requirement.route_data.detail"), ItemStack.EMPTY, 0, 1, false));
      }
      List<TerminalMissionRequirement> requirements = new ArrayList<>();
      boolean correctVehicle = vehicle != null && route.acceptsVehicle(vehicle.kind());
      requirements.add(TerminalMissionRequirement.custom(
         route.requiredVehicle().replace('_', ' '),
         tr("requirement.vehicle_class.detail"),
         vehicleIcon(route),
         correctVehicle ? 1 : 0,
         1,
         correctVehicle
      ));
      int fuel = vehicle == null ? 0 : vehicle.fuel();
      int requiredFuel = ConvoyRouteService.requiredFuel(route);
      requirements.add(TerminalMissionRequirement.custom(
         tr("requirement.fuel"),
         tr("requirement.fuel.detail"),
         new ItemStack(ModItems.FUEL_CANISTER.get()),
         fuel,
         requiredFuel,
         fuel >= requiredFuel
      ));
      for (ConvoyRouteDefinition.StackSpec cargo : route.requiredCargo()) {
         int have = vehicle == null ? 0 : vehicle.cargoItemCount(cargo.item());
         requirements.add(TerminalMissionRequirement.item(cargo.stack(), have, cargo.count(), have >= cargo.count()));
      }
      return requirements;
   }

   private static List<TerminalMissionRequirement> closeRequirements(Player player, @Nullable ConvoyRouteDefinition route) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      boolean active = route != null;
      boolean vehicle = nearestVehicle(player, 12.0D) != null;
      boolean marker = nearestSignalMarker(player) != null;
      return List.of(
         TerminalMissionRequirement.custom(tr("requirement.active_route"), tr("requirement.active_route.detail"), new ItemStack(ModBlocks.CONVOY_BEACON.get().asItem()), active ? 1 : 0, 1, active),
         TerminalMissionRequirement.custom(tr("requirement.paired_vehicle"), tr("requirement.paired_vehicle.detail"), vehicle ? vehicleIcon(route) : new ItemStack(ModItems.ROUTE_BEACON.get()), vehicle ? 1 : 0, 1, vehicle),
         TerminalMissionRequirement.block(tr("requirement.signal_marker"), tr("requirement.signal_marker.detail"), new ItemStack(ModBlocks.ROADSIDE_SIGNAL_MARKER.get().asItem()), marker ? 1 : 0, 1, marker),
         TerminalMissionRequirement.custom(tr("requirement.reward_state"), tr("requirement.reward_state.detail"), new ItemStack(ModItems.CARGO_NET.get()), ConvoyRouteService.claimableRewards(player) > 0 || allRoutesClaimed(progress) ? 1 : 0, 1, ConvoyRouteService.claimableRewards(player) > 0 || allRoutesClaimed(progress))
      );
   }

   private static boolean sideComplete(Player player, SideMission side) {
      if (player == null || side == null) {
         return false;
      }
      ConvoyProgress progress = ConvoyProgress.get(player);
      return switch (side.key()) {
         case "depot_formation" -> progress.flag(side.key()) || hasNearbyBlock(player, ModBlocks.CONVOY_DEPOT_CONTROLLER.get(), 16);
         case "field_operation_staging" -> progress.flag(side.key()) || nearestConvoyController(player, 24.0D)
            .map(controller -> !controller.fieldOperation().routeId().isBlank())
            .orElse(false);
         case "salvage_export" -> progress.flag(side.key()) || nearestConvoyController(player, 24.0D)
            .map(controller -> controller.fieldOperation().salvageExported())
            .orElse(false);
         default -> progress.flag(side.key());
      };
   }

   private static ItemStack sideIcon(String key) {
      return switch (key) {
         case "depot_formation" -> new ItemStack(ModBlocks.CONVOY_DEPOT_CONTROLLER.get().asItem());
         case "refuel_repair" -> new ItemStack(ModItems.CONVOY_REPAIR_KIT.get());
         case "field_operation_staging" -> new ItemStack(ModBlocks.ROUTE_DISPATCH_TOWER_CONTROLLER.get().asItem());
         case "incident_resolution" -> new ItemStack(ModItems.ROUTE_BEACON.get());
         case "convoy_recovery" -> new ItemStack(ModBlocks.CONVOY_RECOVERY_BEACON_CONTROLLER.get().asItem());
         case "salvage_export" -> new ItemStack(ModBlocks.CARGO_OUTPUT_CRATE.get().asItem());
         default -> new ItemStack(ModBlocks.CONVOY_BEACON.get().asItem());
      };
   }

   @Nullable
   private static SideMission sideMission(Identifier missionId) {
      return SIDE_MISSIONS.stream().filter(side -> side.id().equals(missionId)).findFirst().orElse(null);
   }

   private static Optional<ConvoyRouteDefinition> activeRoute(Player player) {
      Identifier routeId = Identifier.tryParse(ConvoyProgress.get(player).activeRouteId());
      return routeId == null ? Optional.empty() : ConvoyContent.route(routeId);
   }

   private static Optional<ConvoyRouteDefinition> firstStartableRoute(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      if (!progress.activeRouteId().isBlank()) {
         return Optional.empty();
      }
      ConvoyVehicleEntity vehicle = nearestVehicle(player, 12.0D);
      return ConvoyContent.routes().stream()
         .filter(route -> !progress.completed(route.id()) && !progress.claimed(route.id()))
         .filter(route -> ConvoyRouteService.readiness(player, vehicle, route).ready())
         .findFirst();
   }

   private static Optional<ConvoyRouteDefinition> firstIncompleteRoute(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      return ConvoyContent.routes().stream()
         .filter(route -> !progress.completed(route.id()) && !progress.claimed(route.id()))
         .findFirst();
   }

   private static Optional<ConvoyRouteDefinition> firstClaimableRoute(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      return ConvoyContent.routes().stream()
         .filter(route -> progress.completed(route.id()) && !progress.claimed(route.id()))
         .findFirst();
   }

   private static String routeBlockerHint(Player player) {
      ConvoyVehicleEntity vehicle = nearestVehicle(player, 12.0D);
      return firstIncompleteRoute(player)
         .map(route -> localizedReadinessHint(player, vehicle, route))
         .orElse(tr("route.hint.no_routes"));
   }

   private static String localizedReadinessHint(Player player, @Nullable ConvoyVehicleEntity vehicle, ConvoyRouteDefinition route) {
      ConvoyRouteService.RouteCheck check = ConvoyRouteService.readiness(player, vehicle, route);
      if (check.ready()) {
         return tr("readiness.ready");
      }
      return switch (blockerFor(check.message())) {
         case NO_VEHICLE -> tr("readiness.no_vehicle");
         case WRONG_VEHICLE -> tr("readiness.wrong_vehicle", route.requiredVehicle().replace('_', ' '));
         case FUEL -> tr("readiness.fuel", ConvoyRouteService.requiredFuel(route));
         case CARGO -> tr("readiness.cargo", ConvoyRouteService.cargoManifest(route));
         case CHECKPOINT -> tr("readiness.checkpoint", route.checkpoint().label());
         default -> tr("readiness.generic");
      };
   }

   private static String activeRouteHint(ConvoyRouteDefinition route) {
      return tr("close.hint.active", route.destinationHint());
   }

   private static boolean hasWorkbench(Player player) {
      return inventoryCount(player, ModBlocks.VEHICLE_WORKBENCH.get().asItem()) > 0
         || hasNearbyBlock(player, ModBlocks.VEHICLE_WORKBENCH.get(), 12);
   }

   private static boolean hasAnyRouteProgress(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      return !progress.activeRouteId().isBlank() || hasAnyCompletedRoute(progress);
   }

   private static boolean hasAnyCompletedRoute(ConvoyProgress progress) {
      return ConvoyContent.routes().stream().anyMatch(route -> progress.completed(route.id()) || progress.claimed(route.id()));
   }

   private static boolean allRoutesClaimed(ConvoyProgress progress) {
      return !ConvoyContent.routes().isEmpty() && ConvoyContent.routes().stream().allMatch(route -> progress.claimed(route.id()));
   }

   private static int inventoryCount(Player player, Item item) {
      if (player == null || item == null) {
         return 0;
      }
      int count = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private static boolean hasNearbyBlock(Player player, Block block, int radius) {
      if (player == null || block == null) {
         return false;
      }
      Level level = player.level();
      BlockPos center = player.blockPosition();
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
         if (level.getBlockState(pos).is(block)) {
            return true;
         }
      }
      return false;
   }

   @Nullable
   private static ConvoyVehicleEntity nearestVehicle(Player player, double radius) {
      if (player == null) {
         return null;
      }
      return player.level().getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(player.blockPosition()).inflate(radius, 4.0D, radius)).stream()
         .filter(vehicle -> vehicle.isOwner(player))
         .min(Comparator.comparingDouble(vehicle -> vehicle.distanceToSqr(player)))
         .orElse(null);
   }

   private static Optional<ConvoyMultiblockControllerBlockEntity> nearestConvoyController(Player player, double radius) {
      if (player == null) {
         return Optional.empty();
      }
      Level level = player.level();
      BlockPos center = player.blockPosition();
      ConvoyMultiblockControllerBlockEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      int horizontal = Math.max(1, (int)Math.ceil(radius));
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontal, -8, -horizontal), center.offset(horizontal, 8, horizontal))) {
         if (level.getBlockEntity(pos) instanceof ConvoyMultiblockControllerBlockEntity controller) {
            double distance = pos.distSqr(center);
            if (distance < bestDistance) {
               bestDistance = distance;
               best = controller;
            }
         }
      }
      return Optional.ofNullable(best);
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

   @Nullable
   private static SignalMarker nearestSignalMarker(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return null;
      }
      return nearestSignalMarker(serverPlayer);
   }

   private static double distanceToCenterSqr(BlockPos pos, Player player) {
      double dx = pos.getX() + 0.5D - player.getX();
      double dy = pos.getY() + 0.5D - player.getY();
      double dz = pos.getZ() + 0.5D - player.getZ();
      return dx * dx + dy * dy + dz * dz;
   }

   private static ItemStack vehicleIcon(@Nullable ConvoyRouteDefinition route) {
      if (route == null) {
         return new ItemStack(ModItems.ROUTE_BEACON.get());
      }
      return switch (route.requiredVehicle()) {
         case "scrap_bike" -> new ItemStack(ModItems.SCRAP_BIKE_KIT.get());
         case "wasteland_rover" -> new ItemStack(ModItems.WASTELAND_ROVER_KIT.get());
         case "cargo_crawler" -> new ItemStack(ModItems.CARGO_CRAWLER_KIT.get());
         case "armored_relay_truck" -> new ItemStack(ModItems.ARMORED_RELAY_TRUCK_KIT.get());
         default -> new ItemStack(ModItems.ROUTE_BEACON.get());
      };
   }

   private static ItemStack routeIcon(Player player, @Nullable ConvoyRouteDefinition route) {
      BlockerKind blocker = routeGuidance(player).blocker();
      return switch (blocker) {
         case NO_VEHICLE, WRONG_VEHICLE -> vehicleIcon(route);
         case FUEL -> new ItemStack(ModItems.FUEL_CANISTER.get());
         case CARGO -> route != null && !route.requiredCargo().isEmpty()
            ? route.requiredCargo().getFirst().stack()
            : new ItemStack(ModBlocks.CARGO_ANCHOR.get().asItem());
         case CHECKPOINT -> new ItemStack(ModItems.ROUTE_BEACON.get());
         case ACTIVE, SIGNAL -> new ItemStack(ModBlocks.ROADSIDE_SIGNAL_MARKER.get().asItem());
         case REWARD -> new ItemStack(ModItems.CARGO_NET.get());
         case READY -> new ItemStack(ModBlocks.CONVOY_BEACON.get().asItem());
         case NONE, BLOCKED -> new ItemStack(ModBlocks.CONVOY_BEACON.get().asItem());
      };
   }

   private static ItemStack closeIcon(Player player, @Nullable ConvoyRouteDefinition route) {
      return switch (closeTone(player)) {
         case REWARD -> new ItemStack(ModItems.CARGO_NET.get());
         case ACTIVE, SIGNAL -> new ItemStack(ModBlocks.ROADSIDE_SIGNAL_MARKER.get().asItem());
         case READY -> new ItemStack(ModBlocks.CONVOY_BEACON.get().asItem());
         default -> routeIcon(player, route);
      };
   }

   private static RouteGuidance routeGuidance(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      if (!progress.activeRouteId().isBlank()) {
         return new RouteGuidance(activeRoute(player), null, BlockerKind.ACTIVE);
      }
      Optional<ConvoyRouteDefinition> route = firstIncompleteRoute(player);
      if (route.isEmpty()) {
         return new RouteGuidance(Optional.empty(), null, BlockerKind.NONE);
      }
      ConvoyVehicleEntity vehicle = nearestVehicle(player, 12.0D);
      ConvoyRouteService.RouteCheck check = ConvoyRouteService.readiness(player, vehicle, route.get());
      return new RouteGuidance(route, check, check.ready() ? BlockerKind.READY : blockerFor(check.message()));
   }

   private static BlockerKind blockerFor(String message) {
      String value = message == null ? "" : message.toLowerCase(Locale.ROOT);
      if (value.contains("no convoy vehicle")) {
         return BlockerKind.NO_VEHICLE;
      }
      if (value.contains("locked") || value.startsWith("requires ")) {
         return BlockerKind.WRONG_VEHICLE;
      }
      if (value.startsWith("fuel ")) {
         return BlockerKind.FUEL;
      }
      if (value.startsWith("missing cargo ")) {
         return BlockerKind.CARGO;
      }
      if (value.contains("reputation")) {
         return BlockerKind.CHECKPOINT;
      }
      return BlockerKind.BLOCKED;
   }

   private static BlockerKind closeTone(Player player) {
      ConvoyProgress progress = ConvoyProgress.get(player);
      if (firstClaimableRoute(player).isPresent()) {
         return BlockerKind.REWARD;
      }
      if (activeRoute(player).isPresent()) {
         return nearestSignalMarker(player) == null ? BlockerKind.ACTIVE : BlockerKind.SIGNAL;
      }
      if (allRoutesClaimed(progress)) {
         return BlockerKind.READY;
      }
      return BlockerKind.NONE;
   }

   private static String missionTone(Player player, Identifier missionId, @Nullable TerminalMissionSnapshot snapshot) {
      if (snapshot != null && (snapshot.status() == TerminalMissionStatus.CLAIMABLE
         || snapshot.status() == TerminalMissionStatus.COMPLETED
         || snapshot.status() == TerminalMissionStatus.CLAIMED)) {
         return "success";
      }
      if (START_ROUTE.equals(missionId)) {
         return routeGuidance(player).blocker().visualTone();
      }
      if (CLOSE_ROUTE.equals(missionId)) {
         return closeTone(player).visualTone();
      }
      return snapshot != null && snapshot.status() == TerminalMissionStatus.UNLOCKED ? "crafting" : "muted";
   }

   private static String tr(String key, Object... args) {
      return Component.translatable(COPY + key, args).getString();
   }

   private record RouteGuidance(Optional<ConvoyRouteDefinition> route, ConvoyRouteService.RouteCheck check, BlockerKind blocker) {
   }

   private record SideMission(
      Identifier id,
      String key,
      String title,
      String briefing,
      String guide,
      String category,
      String difficulty,
      int order,
      String requirement
   ) {
   }

   private enum BlockerKind {
      NO_VEHICLE("blocker.no_vehicle", "route_hint.no_vehicle", "warning", "crafting", "vehicle_missing", TerminalVisualAssets.MISSION_CRAFTING),
      WRONG_VEHICLE("blocker.wrong_vehicle", "route_hint.wrong_vehicle", "warning", "crafting", "vehicle_mismatch", TerminalVisualAssets.MISSION_CRAFTING),
      FUEL("blocker.fuel", "route_hint.fuel", "warning", "tech", "fuel_gate", TerminalVisualAssets.MISSION_TECH),
      CARGO("blocker.cargo", "route_hint.cargo", "warning", "route_prep", "cargo_manifest", TerminalVisualAssets.MISSION_EXPLORATION),
      CHECKPOINT("blocker.checkpoint", "route_hint.checkpoint", "hazard", "route_prep", "checkpoint", TerminalVisualAssets.MISSION_COMBAT),
      ACTIVE("blocker.active", "route_hint.active", "active", "route_prep", "active_route", TerminalVisualAssets.MISSION_EXPLORATION),
      SIGNAL("blocker.signal", "route_hint.signal", "active", "route_prep", "signal_marker", TerminalVisualAssets.MISSION_EXPLORATION),
      REWARD("blocker.reward", "route_hint.reward", "success", "route_prep", "reward_ready", TerminalVisualAssets.MISSION_SIDE_OPS),
      READY("blocker.ready", "route_hint.ready", "success", "route_prep", "ready", TerminalVisualAssets.MISSION_EXPLORATION),
      BLOCKED("blocker.blocked", "route_hint.blocked", "warning", "route_prep", "blocked", TerminalVisualAssets.MISSION_SIDE_OPS),
      NONE("blocker.none", "route_hint.convoy_routes", "muted", "side_ops", "standard", TerminalVisualAssets.MISSION_SIDE_OPS);

      private final String labelKey;
      private final String routeHintKey;
      private final String visualTone;
      private final String trackType;
      private final String heroVariant;
      private final Identifier categoryArt;

      BlockerKind(String labelKey, String routeHintKey, String visualTone, String trackType, String heroVariant, Identifier categoryArt) {
         this.labelKey = labelKey;
         this.routeHintKey = routeHintKey;
         this.visualTone = visualTone;
         this.trackType = trackType;
         this.heroVariant = heroVariant;
         this.categoryArt = categoryArt;
      }

      String label() {
         return tr(labelKey);
      }

      String routeHint() {
         return tr(routeHintKey);
      }

      String visualTone() {
         return visualTone;
      }

      String trackType() {
         return trackType;
      }

      String heroVariant() {
         return heroVariant;
      }

      Identifier categoryArt() {
         return categoryArt;
      }
   }

   private record SignalMarker(BlockPos pos, @Nullable ConvoyStationBlockEntity station) {
   }
}
