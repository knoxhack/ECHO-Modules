package com.knoxhack.echoconvoyprotocol.command;

import com.knoxhack.echoconvoyprotocol.Config;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyHoloMapProvider;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyLogisticsIntegration;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyMissionHooks;
import com.knoxhack.echoconvoyprotocol.task.ConvoyMultiblockTasks;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.IMapMarker;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class ConvoyCommands {
   private ConvoyCommands() {
   }

   public static void register(Object event) {
      CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
      if (dispatcher == null) {
         return;
      }
      var root = Commands.literal("echo_convoy")
         .requires(ConvoyCommands::isGamemaster)
         .then(Commands.literal("status").executes(context -> status(context.getSource())))
         .then(Commands.literal("routes").executes(context -> routes(context.getSource())))
         .then(Commands.literal("vehicle").executes(context -> vehicle(context.getSource())))
         .then(Commands.literal("readiness").executes(context -> readiness(context.getSource())))
         .then(Commands.literal("logistics").executes(context -> logistics(context.getSource())))
         .then(Commands.literal("markers").executes(context -> markers(context.getSource())))
         .then(Commands.literal("ops")
            .executes(context -> ops(context.getSource()))
            .then(Commands.literal("launch")
               .then(Commands.argument("route", StringArgumentType.word()).executes(context -> opsLaunch(
                  context.getSource(),
                  StringArgumentType.getString(context, "route")
               ))))
            .then(Commands.literal("recall").executes(context -> opsRecall(context.getSource())))
            .then(Commands.literal("resolve").executes(context -> opsResolve(context.getSource())))
            .then(Commands.literal("recover").executes(context -> opsRecover(context.getSource()))))
         .then(Commands.literal("dispatch")
            .then(Commands.argument("route", StringArgumentType.word()).executes(context -> dispatch(
               context.getSource(),
               StringArgumentType.getString(context, "route")
            ))))
         .then(Commands.literal("complete")
            .then(Commands.argument("route", StringArgumentType.word()).executes(context -> complete(
               context.getSource(),
               StringArgumentType.getString(context, "route")
            ))))
         .then(Commands.literal("recover").executes(context -> recover(context.getSource())))
         .then(Commands.literal("depot")
            .then(Commands.literal("info").executes(context -> depotInfo(context.getSource())))
            .then(Commands.literal("validate").executes(context -> depotValidate(context.getSource()))))
         .then(Commands.literal("task")
            .then(Commands.literal("start")
               .then(Commands.argument("task", StringArgumentType.word()).executes(context -> taskStart(
                  context.getSource(),
                  StringArgumentType.getString(context, "task")
               ))))
            .then(Commands.literal("clear").executes(context -> taskClear(context.getSource()))));
      dispatcher.register(root);
      dispatcher.register(Commands.literal("echoconvoy")
         .requires(ConvoyCommands::isGamemaster)
         .redirect(dispatcher.getRoot().getChild("echo_convoy")));
   }

   @SuppressWarnings("unchecked")
   private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
      if (event == null) {
         return null;
      }
      try {
         Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
         return dispatcher instanceof CommandDispatcher<?> liveDispatcher
            ? (CommandDispatcher<CommandSourceStack>)liveDispatcher
            : null;
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return null;
      }
   }

   private static boolean isGamemaster(CommandSourceStack source) {
      return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
   }

   private static int status(CommandSourceStack source) {
      source.sendSuccess(() -> Component.literal("ECHO Convoy Protocol online. Routes loaded: " + ConvoyContent.routes().size()
         + " | policy=" + Config.vehicleJoinPolicy()
         + " | routeDifficulty=" + Config.routeDifficultyMultiplier()
         + " | fieldOpsScale=" + Config.fieldOpsDurationScale()
         + " | incidents=" + Config.incidentFrequencyPercent() + "%"
         + " | debug=" + Config.debugDiagnostics()), false);
      return Command.SINGLE_SUCCESS;
   }

   private static int routes(CommandSourceStack source) {
      String routes = ConvoyContent.routes().stream()
         .map(route -> route.id() + " [" + route.missionType() + ", logistics=" + route.logisticsNetworkId()
            + "/" + (route.logisticsLoadoutId() == null ? "none" : route.logisticsLoadoutId()) + "]")
         .sorted()
         .reduce((left, right) -> left + "; " + right)
         .orElse("none");
      source.sendSuccess(() -> Component.literal("Convoy routes: " + routes), false);
      return Command.SINGLE_SUCCESS;
   }

   private static int vehicle(CommandSourceStack source) {
      ConvoyVehicleEntity vehicle = nearestVehicle(source);
      if (vehicle == null) {
         return say(source, "No Convoy vehicle nearby.");
      }
      source.sendSuccess(() -> Component.literal("Convoy vehicle: " + vehicle.callsign()
         + " kind=" + vehicle.kind().getSerializedName()
         + " fuel=" + vehicle.fuel() + "/" + vehicle.maxFuel()
         + " battery=" + vehicle.battery() + "/" + vehicle.maxBattery()
         + " damage=" + vehicle.damage() + "/" + vehicle.maxDamage()), false);
      source.sendSuccess(() -> Component.literal("Convoy vehicle state: cargo=" + vehicle.filledCargoSlots() + "/" + vehicle.cargoSlots()
         + " route=" + (vehicle.activeRouteId().isBlank() ? "none" : vehicle.activeRouteId())
         + " docked=" + vehicle.docked()
         + " shielding=" + vehicle.shieldingPlates()
         + " owner=" + (vehicle.ownerId() == null ? "unclaimed" : vehicle.ownerId())), false);
      return Command.SINGLE_SUCCESS;
   }

   private static int depotInfo(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      controller.diagnosticLines().forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
      return Command.SINGLE_SUCCESS;
   }

   private static int depotValidate(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      var result = controller.validateStructure();
      if (result.valid()) {
         controller.onStructureFormed();
      }
      source.sendSuccess(() -> Component.literal("Convoy validation: " + (result.valid() ? "formed" : "blocked")
         + " | completion " + Math.round(result.completion() * 100.0D) + "%"), false);
      if (!result.valid()) {
         result.groupedIssues().forEach(issue -> source.sendSuccess(() -> Component.literal(
            "- " + issue.count() + "x " + issue.kind() + " " + issue.expected()
         ), false));
      }
      return Command.SINGLE_SUCCESS;
   }

   private static int readiness(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      source.sendSuccess(() -> Component.literal(controller.readiness().summaryLine()), false);
      return Command.SINGLE_SUCCESS;
   }

   private static int logistics(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      ConvoyRouteDefinition route = selectedRoute(controller);
      ConvoyLogisticsIntegration.LogisticsStatus status = ConvoyLogisticsIntegration.syncInventory(controller, route);
      source.sendSuccess(() -> Component.literal("Convoy Logistics: network=" + controller.convoyState().logisticsNetworkId()
         + ", loadout=" + (controller.convoyState().logisticsLoadoutId().isBlank() ? "none" : controller.convoyState().logisticsLoadoutId())
         + ", available=" + status.available()
         + ", online=" + status.networkOnline()
         + ", ready=" + status.loadoutReady()
         + ", active deliveries=" + status.activeDeliveries()
         + ", request active=" + controller.convoyState().logisticsCargoRequestActive()
         + ", delivered=" + controller.convoyState().logisticsCargoDelivered()), false);
      return Command.SINGLE_SUCCESS;
   }

   private static int ops(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      source.sendSuccess(() -> Component.literal(controller.fieldOperation().summaryLine()), false);
      source.sendSuccess(() -> Component.literal("Field Ops diagnostic: " + controller.fieldOperation().lastDiagnostic()), false);
      if (!controller.fieldOperation().failureReason().isBlank()) {
         source.sendSuccess(() -> Component.literal("Field Ops blocker: " + controller.fieldOperation().failureReason()), false);
      }
      return Command.SINGLE_SUCCESS;
   }

   private static int opsLaunch(CommandSourceStack source, String rawRoute) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      ConvoyRouteDefinition route = route(rawRoute);
      if (route == null) {
         return say(source, "Unknown route: " + rawRoute);
      }
      controller.convoyState().prepareRoute(route);
      controller.fieldOperation().stage(route, source.getLevel().getGameTime());
      boolean launched = controller.fieldOperation().launch(route, controller.convoyState(), source.getLevel().getGameTime());
      if (launched && source.getPlayer() != null) {
         ConvoyMissionHooks.recordFieldOperationStaged(source.getPlayer(), route.id());
      }
      return say(source, launched ? controller.fieldOperation().summaryLine() : controller.fieldOperation().lastDiagnostic());
   }

   private static int opsRecall(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      controller.fieldOperation().recall(controller.convoyState());
      return say(source, controller.fieldOperation().lastDiagnostic());
   }

   private static int opsResolve(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      Identifier incident = Identifier.tryParse(controller.fieldOperation().incidentId());
      if (controller.fieldOperation().resolveIncident(controller.convoyState()) && source.getPlayer() != null) {
         ConvoyMissionHooks.recordIncidentResolved(source.getPlayer(), incident);
      }
      return say(source, controller.fieldOperation().lastDiagnostic());
   }

   private static int opsRecover(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      if (controller.fieldOperation().recover(controller.convoyState()) && source.getPlayer() != null) {
         ConvoyMissionHooks.recordRecovery(source.getPlayer(), "field_operation");
      }
      return say(source, controller.fieldOperation().lastDiagnostic());
   }

   private static int markers(CommandSourceStack source) {
      ServerPlayer player = source.getPlayer();
      if (player == null) {
         return say(source, "Marker listing requires a player source.");
      }
      List<IMapMarker> markers = EchoCoreServices.mapMarkers(player).stream()
         .filter(marker -> ConvoyHoloMapProvider.INSTANCE.providerId().equals(marker.sourceId()))
         .toList();
      long routeLines = markers.stream().filter(marker -> marker.routeId() != null).map(IMapMarker::routeId).distinct().count();
      long precise = markers.stream().filter(IMapMarker::precise).count();
      source.sendSuccess(() -> Component.literal("Convoy markers: " + markers.size()
         + " | route lines=" + routeLines
         + " | precise=" + precise
         + " | fallback=" + (markers.size() - precise)), false);
      markers.stream()
         .sorted(Comparator.comparing(marker -> marker.layerId().toString()))
         .limit(8)
         .forEach(marker -> source.sendSuccess(() -> Component.literal("- " + marker.title()
            + " [" + marker.layerId() + "] route=" + (marker.routeId() == null ? "none" : marker.routeId())
            + " order=" + marker.routeOrder()
            + " precise=" + marker.precise()), false));
      return Command.SINGLE_SUCCESS;
   }

   private static int dispatch(CommandSourceStack source, String rawRoute) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      ConvoyRouteDefinition route = route(rawRoute);
      if (route == null) {
         return say(source, "Unknown route: " + rawRoute);
      }
      controller.convoyState().prepareRoute(route);
      if (route.autoRequestCargo() && !controller.convoyState().logisticsCargoDelivered()) {
         boolean requested = ConvoyLogisticsIntegration.requestRouteSupplies(controller, route);
         return say(source, requested
            ? "Route supplies requested for " + route.id() + "; dispatch will unlock after delivery/sync."
            : "Route prepared, but Logistics request is unavailable. Fill cargo and fuel locally.");
      }
      boolean dispatched = controller.convoyState().dispatch();
      return say(source, dispatched ? "Convoy dispatched to " + route.id() + "." : controller.convoyState().lastDiagnostic());
   }

   private static int complete(CommandSourceStack source, String rawRoute) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      controller.convoyState().completeActiveRoute();
      boolean exported = ConvoyLogisticsIntegration.exportSalvageManifest(controller, route(rawRoute));
      if (exported && source.getPlayer() != null) {
         ConvoyMissionHooks.recordSalvageExport(source.getPlayer(), route(rawRoute) == null ? null : route(rawRoute).id());
      }
      return say(source, controller.convoyState().lastDiagnostic());
   }

   private static int recover(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      controller.convoyState().recoverConvoy();
      if (source.getPlayer() != null) {
         ConvoyMissionHooks.recordRecovery(source.getPlayer(), "convoy_state");
      }
      return say(source, controller.convoyState().lastDiagnostic());
   }

   private static int taskStart(CommandSourceStack source, String task) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      Player player = source.getPlayer();
      boolean started = controller.queueTask(ConvoyMultiblockTasks.taskId(task), player);
      return say(source, started
         ? "Task queued: " + task + ". Controller " + controller.statusSnapshot().state().name().toLowerCase(java.util.Locale.ROOT) + "."
         : "Task blocked: " + task + ". " + controller.diagnosticLines().stream().findFirst().orElse("Run /echo_convoy depot validate for details."));
   }

   private static int taskClear(CommandSourceStack source) {
      ConvoyMultiblockControllerBlockEntity controller = nearestController(source);
      if (controller == null) {
         return say(source, "No Convoy multiblock controller nearby.");
      }
      controller.clearTask(source.getPlayer());
      return Command.SINGLE_SUCCESS;
   }

   private static ConvoyMultiblockControllerBlockEntity nearestController(CommandSourceStack source) {
      ServerLevel level = source.getLevel();
      BlockPos origin = BlockPos.containing(source.getPosition());
      ConvoyMultiblockControllerBlockEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -6, -12), origin.offset(12, 6, 12))) {
         if (level.getBlockEntity(pos) instanceof ConvoyMultiblockControllerBlockEntity controller) {
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
               bestDistance = distance;
               best = controller;
            }
         }
      }
      return best;
   }

   private static ConvoyVehicleEntity nearestVehicle(CommandSourceStack source) {
      ServerLevel level = source.getLevel();
      BlockPos origin = BlockPos.containing(source.getPosition());
      ConvoyVehicleEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (ConvoyVehicleEntity vehicle : level.getEntitiesOfClass(ConvoyVehicleEntity.class, new AABB(origin).inflate(16.0D))) {
         double distance = vehicle.blockPosition().distSqr(origin);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = vehicle;
         }
      }
      return best;
   }

   private static ConvoyRouteDefinition selectedRoute(ConvoyMultiblockControllerBlockEntity controller) {
      Identifier id = Identifier.tryParse(controller.convoyState().routeId());
      return id == null ? ConvoyContent.firstRoute().orElse(null) : ConvoyContent.route(id).orElse(null);
   }

   private static ConvoyRouteDefinition route(String raw) {
      Identifier id = raw == null || raw.contains(":")
         ? Identifier.tryParse(raw == null ? "" : raw)
         : Identifier.fromNamespaceAndPath("echoconvoyprotocol", raw);
      return id == null ? null : ConvoyContent.route(id).orElse(null);
   }

   private static int say(CommandSourceStack source, String line) {
      source.sendSuccess(() -> Component.literal(line == null ? "" : line), false);
      return Command.SINGLE_SUCCESS;
   }
}
